package net.dvmn2.cameraManagerPlugin.shake;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ShakeCommand {

    // Значения по умолчанию для необязательных параметров команды —
    // применяются, если соответствующий аргумент не указан при вызове.
    private static final int DEFAULT_ANGLE_DELTA = 20;
    private static final int DEFAULT_POSITION_DELTA = 20;
    private static final int DEFAULT_DURATION = 20;

    private static final String[] MODES = {"add", "stop"};

    // ВАЖНО: должно совпадать с идентификатором пакета в моде
    // (CameraShakePayload.ID -> Identifier.of("cameramanager", "shake")).
    public static final String CHANNEL = "cameramanager:shake";

    /**
     * Строит дерево команды через Brigadier/Paper Command API.
     * Все три числовых аргумента ограничены снизу нулём — отрицательная
     * амплитуда/длительность не имеет смысла.
     */
    public static ArgumentBuilder<CommandSourceStack, ?> create(JavaPlugin plugin) {
        return Commands.literal("shake")
                .requires(source -> source.getSender().hasPermission("cameramanager.shake.admin"))
                .executes(ctx -> run(ctx,
                        (List<Player>) ctx.getSource().getSender(),
                        DEFAULT_ANGLE_DELTA,
                        DEFAULT_POSITION_DELTA,
                        DEFAULT_DURATION,
                        plugin
                ))
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .executes(ctx -> run(ctx,
                                resolvePlayers(ctx),
                                DEFAULT_ANGLE_DELTA,
                                DEFAULT_POSITION_DELTA,
                                DEFAULT_DURATION,
                                plugin
                        ))
                        .then(Commands.argument("angle_delta", IntegerArgumentType.integer(0))
                                .executes(ctx -> run(ctx,
                                        resolvePlayers(ctx),
                                        IntegerArgumentType.getInteger(ctx, "angle_delta"),
                                        DEFAULT_POSITION_DELTA,
                                        DEFAULT_DURATION,
                                        plugin
                                ))
                                .then(Commands.argument("position_delta", IntegerArgumentType.integer(0))
                                        .executes(ctx -> run(ctx,
                                                resolvePlayers(ctx),
                                                IntegerArgumentType.getInteger(ctx, "angle_delta"),
                                                IntegerArgumentType.getInteger(ctx, "position_delta"),
                                                DEFAULT_DURATION,
                                                plugin
                                        ))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                .executes(ctx -> run(ctx,
                                                        resolvePlayers(ctx),
                                                        IntegerArgumentType.getInteger(ctx, "angle_delta"),
                                                        IntegerArgumentType.getInteger(ctx, "position_delta"),
                                                        IntegerArgumentType.getInteger(ctx, "duration"),
                                                        plugin
                                                ))))));
    }

    private static List<Player> resolvePlayers(CommandContext<CommandSourceStack> ctx) {
        try {
            PlayerSelectorArgumentResolver resolver =
                    ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            return resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException | IndexOutOfBoundsException e) {
            ctx.getSource().getSender().sendMessage("§cИгрок не найден.");
            return null;
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx, List<Player> players,
                           int angle_delta, int position_delta, int duration, JavaPlugin plugin) throws CommandSyntaxException {

        if (players.isEmpty()) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Не найдено ни одного игрока для тряски камеры."));
            return 0;
        }

        // Формируем "сырые" байты пакета. Порядок записи полей должен
        // строго совпадать с порядком чтения в CameraShakePayload.CODEC
        // на клиенте — это единственное, что связывает плагин и мод,
        // общего кода/зависимости между ними нет.
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(angle_delta);
        out.writeInt(position_delta);
        out.writeInt(duration);
        byte[] data = out.toByteArray();

        StringBuilder playersNames = new StringBuilder();

        for (Player player : players) {
            // Отправляем данные через стандартный канал Bukkit Plugin Messaging.
            // Мод на клиенте получает их как обычный Fabric S2C-пакет, так как
            // имя канала совпадает с идентификатором CameraShakePayload.ID.
            player.sendPluginMessage(plugin, CHANNEL, data);

            if (!playersNames.isEmpty()) {
                playersNames.append(", ");
            }
            playersNames.append(player.getName());
        }

        // Короткая обратная связь отправителю команды (админу/консоли).
        ctx.getSource().getSender().sendMessage(
                Component.text("Тряска камеры отправлена: " + playersNames));

        return Command.SINGLE_SUCCESS;
    }
}
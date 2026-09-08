package net.dvmn2.cameraManagerPlugin.shake;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Команда {@code /shake <targets> <angle_delta> <position_delta> <duration>}.
 * <p>
 * Отправляет выбранным игрокам plugin-message с параметрами тряски камеры.
 * Реальную тряску применяет клиентский мод CameraManager (см. {@code CameraShakeHandler}
 * и {@code ShakeMixin} в клиентском модуле) — плагин лишь передаёт данные.
 * Если у игрока мод не установлен, канал просто игнорируется клиентом, ничего
 * не сломается.
 */
public class ShakeCommand {

    // ВАЖНО: должно совпадать с идентификатором пакета в моде
    // (CameraShakePayload.ID -> Identifier.of("cameramanager", "shake")).
    public static final String CHANNEL = "cameramanager:shake";

    /**
     * Строит дерево команды через Brigadier/Paper Command API.
     * Все три числовых аргумента ограничены снизу нулём — отрицательная
     * амплитуда/длительность не имеет смысла.
     */
    public static LiteralCommandNode<CommandSourceStack> create(JavaPlugin plugin) {
        return Commands.literal("shake")
                .requires(source -> source.getSender().hasPermission("cameramanager.shake.admin"))
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .then(Commands.argument("angle_delta", IntegerArgumentType.integer(0))
                                .then(Commands.argument("position_delta", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                                .executes(ctx -> run(ctx, plugin))))))
                .build();
    }

    private static int run(CommandContext<CommandSourceStack> ctx, JavaPlugin plugin) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver =
                ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);

        // Ключевой момент: резолвим селектор через ctx.getSource() —
        // у CommandSourceStack уже подставлена корректная location из
        // /execute at, что важно для позиционных селекторов
        // (например, @a[distance=..] при вызове через /execute).
        List<Player> players = resolver.resolve(ctx.getSource());

        int angle_delta = IntegerArgumentType.getInteger(ctx, "angle_delta");
        int position_delta = IntegerArgumentType.getInteger(ctx, "position_delta");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");

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
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
import net.dvmn2.cameraManagerPlugin.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Команда /camera shake — управляет тряской камеры у игроков через plugin-messaging.
 * Command /camera shake — drives player camera shake via plugin-messaging.
 * <p>
 * /camera shake add [targets] [angle_delta] [position_delta] [duration]
 * /camera shake stop [targets]
 */
public class ShakeCommand {

    // Значения по умолчанию, если аргументы не указаны явно.
    // Default values used when arguments are omitted.
    private static final int DEFAULT_ANGLE_DELTA = 20;
    private static final int DEFAULT_POSITION_DELTA = 20;
    private static final int DEFAULT_DURATION = 20; // в тиках (20 тиков = 1 секунда) / in ticks (20 ticks = 1 second)

    // Названия каналов plugin-messaging. ВАЖНО: должны точно совпадать
    // с Identifier'ами CustomPayload на клиенте (CameraShakePayload.ID / CameraShakeStopPayload.ID).
    //
    // Plugin-messaging channel names. IMPORTANT: must exactly match the client's
    // CustomPayload identifiers (CameraShakePayload.ID / CameraShakeStopPayload.ID).
    public static final String CHANNEL_ADD = "cameramanager:shake";
    public static final String CHANNEL_STOP = "cameramanager:shake_stop";

    public static ArgumentBuilder<CommandSourceStack, ?> create(JavaPlugin plugin) {
        return Commands.literal("shake")
                .requires(source -> source.getSender().hasPermission("cameramanager.shake.admin"))
                .then(buildAddBranch(plugin))
                .then(buildStopBranch(plugin));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildAddBranch(JavaPlugin plugin) {
        return Commands.literal("add")
                .executes(ctx -> add(ctx,
                        resolveSelfOrSender(ctx),
                        DEFAULT_ANGLE_DELTA,
                        DEFAULT_POSITION_DELTA,
                        DEFAULT_DURATION,
                        plugin
                ))
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .executes(ctx -> add(ctx,
                                resolvePlayers(ctx),
                                DEFAULT_ANGLE_DELTA,
                                DEFAULT_POSITION_DELTA,
                                DEFAULT_DURATION,
                                plugin
                        ))
                        .then(Commands.argument("angle_delta", IntegerArgumentType.integer(0))
                                .executes(ctx -> add(ctx,
                                        resolvePlayers(ctx),
                                        IntegerArgumentType.getInteger(ctx, "angle_delta"),
                                        DEFAULT_POSITION_DELTA,
                                        DEFAULT_DURATION,
                                        plugin
                                ))
                                .then(Commands.argument("position_delta", IntegerArgumentType.integer(0))
                                        .executes(ctx -> add(ctx,
                                                resolvePlayers(ctx),
                                                IntegerArgumentType.getInteger(ctx, "angle_delta"),
                                                IntegerArgumentType.getInteger(ctx, "position_delta"),
                                                DEFAULT_DURATION,
                                                plugin
                                        ))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                .executes(ctx -> add(ctx,
                                                        resolvePlayers(ctx),
                                                        IntegerArgumentType.getInteger(ctx, "angle_delta"),
                                                        IntegerArgumentType.getInteger(ctx, "position_delta"),
                                                        IntegerArgumentType.getInteger(ctx, "duration"),
                                                        plugin
                                                ))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildStopBranch(JavaPlugin plugin) {
        return Commands.literal("stop")
                .executes(ctx -> stop(ctx, resolveSelfOrSender(ctx), plugin))
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .executes(ctx -> stop(ctx, resolvePlayers(ctx), plugin)));
    }

    /** Разбирает селектор targets в список игроков. / Resolves the targets selector into a player list. */
    private static List<Player> resolvePlayers(CommandContext<CommandSourceStack> ctx) {
        try {
            PlayerSelectorArgumentResolver resolver =
                    ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
            return resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException | IndexOutOfBoundsException e) {
            ctx.getSource().getSender().sendMessage(Lang.get(Lang.Key.PLAYER_NOT_FOUND, ctx.getSource().getSender()));
            return Collections.emptyList();
        }
    }

    /**
     * Если targets не указаны — берём самого отправителя (если это игрок).
     * When targets are omitted — fall back to the sender themself (if they're a player).
     */
    private static List<Player> resolveSelfOrSender(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            return List.of(player);
        }
        ctx.getSource().getSender().sendMessage(Lang.get(Lang.Key.SPECIFY_TARGETS, ctx.getSource().getSender()));
        return Collections.emptyList();
    }

    private static int add(CommandContext<CommandSourceStack> ctx, List<Player> players,
                           int angle_delta, int position_delta, int duration, JavaPlugin plugin) {
        if (players == null || players.isEmpty()) {
            ctx.getSource().getSender().sendMessage(
                    Component.text(Lang.get(Lang.Key.NO_PLAYERS_FOR_SHAKE, ctx.getSource().getSender())));
            return 0;
        }

        // Формат пакета: int angle_delta, int position_delta, int duration.
        // Должен побайтово совпадать с CameraShakePayload.CODEC на клиенте!
        //
        // Packet layout: int angle_delta, int position_delta, int duration.
        // Must match CameraShakePayload.CODEC on the client byte-for-byte!
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(angle_delta);
        out.writeInt(position_delta);
        out.writeInt(duration);

        return send(ctx, players, plugin, CHANNEL_ADD, out.toByteArray(),
                Lang.get(Lang.Key.SHAKE_SENT, ctx.getSource().getSender()));
    }

    private static int stop(CommandContext<CommandSourceStack> ctx, List<Player> players, JavaPlugin plugin) {
        if (players == null || players.isEmpty()) {
            ctx.getSource().getSender().sendMessage(
                    Component.text(Lang.get(Lang.Key.NO_PLAYERS_FOR_STOP, ctx.getSource().getSender())));
            return 0;
        }

        // У stop-пакета нет полезной нагрузки — совпадает с CameraShakeStopPayload (PacketCodec.unit(...)).
        // The stop packet carries no payload — matches CameraShakeStopPayload (PacketCodec.unit(...)).
        return send(ctx, players, plugin, CHANNEL_STOP, new byte[0],
                Lang.get(Lang.Key.SHAKE_STOPPED, ctx.getSource().getSender()));
    }

    private static int send(CommandContext<CommandSourceStack> ctx, List<Player> players,
                            JavaPlugin plugin, String channel, byte[] data, String feedbackPrefix) {

        StringBuilder playersNames = new StringBuilder();

        for (Player player : players) {
            player.sendPluginMessage(plugin, channel, data);

            if (!playersNames.isEmpty()) {
                playersNames.append(", ");
            }
            playersNames.append(player.getName());
        }

        ctx.getSource().getSender().sendMessage(
                Component.text(feedbackPrefix + playersNames));

        return Command.SINGLE_SUCCESS;
    }
}
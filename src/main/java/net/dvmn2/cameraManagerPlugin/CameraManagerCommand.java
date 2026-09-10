package net.dvmn2.cameraManagerPlugin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.dvmn2.cameraManagerPlugin.shake.ShakeCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CameraManagerCommand {

    public static LiteralCommandNode<CommandSourceStack> create(JavaPlugin plugin) {
        return Commands.literal("camera")
                .requires(source -> source.getSender().hasPermission("cameramanager.admin"))
                .then(ShakeCommand.create(plugin))
                .build();
    }
}

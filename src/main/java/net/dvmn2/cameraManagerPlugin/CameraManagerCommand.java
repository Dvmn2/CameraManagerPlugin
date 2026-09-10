package net.dvmn2.cameraManagerPlugin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.dvmn2.cameraManagerPlugin.shake.ShakeCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Корневая команда /camera. Сама по себе ничего не делает — служит "родителем"
 * для подкоманд вроде /camera shake. Доступ ограничен permission'ом
 * cameramanager.admin (см. plugin.yml — она обязательно должна быть там
 * объявлена, иначе Bukkit по умолчанию разрешит её ВСЕМ игрокам!).
 * <p>
 * Root /camera command. Does nothing by itself — acts as the parent node for
 * subcommands like /camera shake. Access is gated by the cameramanager.admin
 * permission (see plugin.yml — it MUST be declared there, otherwise Bukkit
 * defaults an undeclared permission to allowed for EVERY player!).
 */
public class CameraManagerCommand {

    public static LiteralCommandNode<CommandSourceStack> create(JavaPlugin plugin) {
        return Commands.literal("camera")
                .requires(source -> source.getSender().hasPermission("cameramanager.admin"))
                .then(ShakeCommand.create(plugin))
                .build();
    }
}
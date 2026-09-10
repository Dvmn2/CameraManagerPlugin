package net.dvmn2.cameraManagerPlugin;

import net.dvmn2.cameraManagerPlugin.shake.ShakeCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CameraManagerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, ShakeCommand.CHANNEL);
        getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> event.registrar().register(
                        CameraManagerCommand.create(this),
                        "Управляет камерой игрока"
                )
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("ShakePlugin disabled!");
    }
}

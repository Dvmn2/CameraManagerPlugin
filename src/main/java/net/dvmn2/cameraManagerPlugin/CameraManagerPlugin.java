package net.dvmn2.cameraManagerPlugin;

import net.dvmn2.cameraManagerPlugin.shake.ShakeCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CameraManagerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Загружаем/создаём config.yml по умолчанию (если он есть в ресурсах плагина).
        saveDefaultConfig();

        // Регистрируем исходящий канал, через который будем слать пакеты клиентам.
        // Без регистрации Bukkit заблокирует отправку plugin-message на этот канал.
        getServer().getMessenger().registerOutgoingPluginChannel(this, ShakeCommand.CHANNEL);

        // Регистрируем команду /shake через Lifecycle API Paper (актуальный способ
        // регистрации команд начиная с Paper 1.20.6+, используется и в 1.21.11).
        getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> event.registrar().register(
                        ShakeCommand.create(this),
                        "Трясёт камеру игрока"
                )
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("ShakePlugin disabled!");
    }
}

package net.dvmn2.cameraManagerPlugin;

import net.dvmn2.cameraManagerPlugin.shake.ShakeCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Главный класс серверного плагина CameraManager.
 * Отвечает за регистрацию каналов plugin-messaging и команды /camera.
 * <p>
 * Main class of the CameraManager server plugin.
 * Registers plugin-messaging channels and the /camera command.
 */
public final class CameraManagerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Создаёт config.yml при первом запуске, если его ещё нет.
        // Creates config.yml on first launch if it doesn't exist yet.
        saveDefaultConfig();

        // Читаем язык из конфига (settings.language: "ru" | "en" | "auto")
        // и передаём его в систему локализации сообщений.
        //
        // Read the language from the config (settings.language: "ru" | "en" | "auto")
        // and pass it to the message localization system.
        Lang.setLanguage(getConfig().getString("settings.language", "auto"));

        // Регистрируем исходящие каналы plugin-messaging, по которым сервер
        // отправляет клиентскому моду команды на тряску камеры.
        // Названия каналов ДОЛЖНЫ совпадать с Identifier'ами CustomPayload на клиенте!
        //
        // Register outgoing plugin-messaging channels used to send camera-shake
        // commands to the client mod. Channel names MUST match the client's
        // CustomPayload identifiers!
        getServer().getMessenger().registerOutgoingPluginChannel(this, ShakeCommand.CHANNEL_ADD);
        getServer().getMessenger().registerOutgoingPluginChannel(this, ShakeCommand.CHANNEL_STOP);

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
        // Исправлено: раньше здесь было "ShakePlugin disabled!", что не совпадало
        // с реальным именем плагина (CameraManagerPlugin) и вводило в заблуждение в логах.
        //
        // Fixed: this used to say "ShakePlugin disabled!", which didn't match the
        // plugin's actual name (CameraManagerPlugin) and was misleading in the logs.
        getLogger().info("CameraManagerPlugin disabled!");
    }
}
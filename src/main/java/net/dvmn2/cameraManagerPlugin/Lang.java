package net.dvmn2.cameraManagerPlugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Простая система локализации плагина: русский (ru) и английский (en).
 * <p>
 * Язык задаётся в config.yml (settings.language):
 * - "ru"   — всегда русский;
 * - "en"   — всегда английский;
 * - "auto" (по умолчанию) — язык определяется индивидуально для каждого
 * отправителя команды: для игрока берётся его игровая локаль
 * (Player#locale()), для консоли/прочих — английский.
 * <p>
 * Simple RU/EN localization for the plugin.
 * Language is configured via config.yml (settings.language):
 * - "ru"   — always Russian;
 * - "en"   — always English;
 * - "auto" (default) — resolved per command sender: for a player, use their
 * client locale (Player#locale()); for console/other senders,
 * fall back to English.
 */
public final class Lang {

    /**
     * Ключи всех локализуемых сообщений плагина. / Keys for all localizable plugin messages.
     */
    public enum Key {
        PLAYER_NOT_FOUND,
        SPECIFY_TARGETS,
        NO_PLAYERS_FOR_SHAKE,
        NO_PLAYERS_FOR_STOP,
        SHAKE_SENT,
        SHAKE_STOPPED
    }

    private static final Map<Key, String> RU = new EnumMap<>(Key.class);
    private static final Map<Key, String> EN = new EnumMap<>(Key.class);

    static {
        RU.put(Key.PLAYER_NOT_FOUND, "§cИгрок не найден.");
        RU.put(Key.SPECIFY_TARGETS, "§cУкажите targets — этому отправителю нужно явно задать цель.");
        RU.put(Key.NO_PLAYERS_FOR_SHAKE, "Не найдено ни одного игрока для тряски камеры.");
        RU.put(Key.NO_PLAYERS_FOR_STOP, "Не найдено ни одного игрока для остановки тряски камеры.");
        RU.put(Key.SHAKE_SENT, "Тряска камеры отправлена: ");
        RU.put(Key.SHAKE_STOPPED, "Тряска камеры остановлена: ");

        EN.put(Key.PLAYER_NOT_FOUND, "§cPlayer not found.");
        EN.put(Key.SPECIFY_TARGETS, "§cSpecify targets — this sender must explicitly provide a target.");
        EN.put(Key.NO_PLAYERS_FOR_SHAKE, "No players found to shake the camera for.");
        EN.put(Key.NO_PLAYERS_FOR_STOP, "No players found to stop the camera shake for.");
        EN.put(Key.SHAKE_SENT, "Camera shake sent to: ");
        EN.put(Key.SHAKE_STOPPED, "Camera shake stopped for: ");
    }

    /**
     * "ru", "en" или "auto" — значение из config.yml. / "ru", "en" or "auto" from config.yml.
     */
    private static volatile String configuredLanguage = "auto";

    private Lang() {
    }

    /**
     * Устанавливает язык плагина из конфига.
     * Setting the plugin language from the config.
     */
    public static void setLanguage(String language) {
        if (language == null || language.isBlank()) {
            configuredLanguage = "auto";
            return;
        }
        configuredLanguage = language.toLowerCase(Locale.ROOT);
    }

    /**
     * Возвращает локализованное сообщение для конкретного отправителя команды.
     * Returns the localized message for a specific command sender.
     */
    public static String get(Key key, CommandSender sender) {
        Map<Key, String> table = resolveTable(sender);
        return table.getOrDefault(key, RU.get(key));
    }

    private static Map<Key, String> resolveTable(CommandSender sender) {
        return switch (configuredLanguage) {
            case "ru" -> RU;
            case "en" -> EN;
            default -> autoResolve(sender); // "auto" или некорректное значение
        };
    }

    private static Map<Key, String> autoResolve(CommandSender sender) {
        if (sender instanceof Player player) {
            // Player#locale() отдаёт java.util.Locale игрока, выставленную в настройках клиента.
            // Player#locale() returns the client-configured java.util.Locale.
            String langCode = player.locale().getLanguage();
            return "ru".equalsIgnoreCase(langCode) ? RU : EN;
        }
        // Консоль/RCON и т.п. — по умолчанию английский.
        // Console/RCON etc. — default to English.
        return EN;
    }
}
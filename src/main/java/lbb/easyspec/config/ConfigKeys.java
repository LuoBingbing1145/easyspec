package lbb.easyspec.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registry of all configuration keys.
 * <p>
 * <strong>To add a new config option:</strong> create one {@code ConfigKey}
 * constant here and add it to the {@link #ALL} list. Serialization, validation,
 * command suggestions, and info display all pick it up automatically — nothing
 * else needs to change.
 * <p>
 * Example:
 * <pre>{@code
 *   public static final ConfigKey<Integer> MAX_PLAYERS = new ConfigKey<>(
 *       "maxPlayers", Integer.class, 100,
 *       "Maximum number of players allowed on the server",
 *       v -> v > 0,
 *       Integer::parseInt,
 *       List.of()
 *   );
 *   // ... then add MAX_PLAYERS to the ALL list below
 * }</pre>
 */
public final class ConfigKeys {

    // ---- Supported languages (used by the LANGUAGE key validator) -------------

    private static final List<String> SUPPORTED_LANGUAGES = List.of(
            "en_us", "zh_cn", "ja_jp", "ko_kr", "fr_fr", "de_de", "es_es", "ru_ru", "pt_br"
    );

    // ---- Key constants -------------------------------------------------------

    /** Language code for translations. One of the 9 supported languages. */
    public static final ConfigKey<String> LANGUAGE = new ConfigKey<>(
            "language", String.class, "en_us",
            "Language: en_us, zh_cn, ja_jp, ko_kr, fr_fr, de_de, es_es, ru_ru, pt_br",
            SUPPORTED_LANGUAGES::contains,
            Function.identity(),
            SUPPORTED_LANGUAGES
    );

    /**
     * Trigger word typed after {@code !} in chat to toggle spectator mode.
     * Default is {@code s} (i.e. {@code !s}).
     */
    public static final ConfigKey<String> TRIGGER = new ConfigKey<>(
            "trigger", String.class, "s",
            "Trigger word: type '!' + this in chat to toggle spectator. Default: s (i.e. !s)",
            v -> v != null && !v.isBlank(),
            String::trim,
            List.of()
    );

    /**
     * Whether to hide the trigger message from chat.
     * When {@code true}, the trigger message is not broadcast to other players.
     */
    public static final ConfigKey<Boolean> HIDE_TRIGGER = new ConfigKey<>(
            "hideTrigger", Boolean.class, false,
            "Hide the trigger message from chat (default: false). Set true to hide it so other players don't see it in chat.",
            null, // any boolean is valid
            v -> {
                if ("true".equalsIgnoreCase(v)) return true;
                if ("false".equalsIgnoreCase(v)) return false;
                throw new IllegalArgumentException("must be 'true' or 'false'");
            },
            List.of("true", "false")
    );

    /**
     * Required Minecraft permission level (0-4) for {@code /easyspec} commands.
     * Default is 2 (operator).
     */
    public static final ConfigKey<Integer> PERMISSION_LEVEL = new ConfigKey<>(
            "permissionLevel", Integer.class, 2,
            "Required permission level for /easyspec commands (0-4). Default: 2 (operator). Set 0 for all players.",
            v -> v >= 0 && v <= 4,
            v -> {
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("must be an integer between 0 and 4");
                }
            },
            List.of("0", "1", "2", "3", "4")
    );

    /**
     * Required Minecraft permission level (0-4) for using the trigger word in chat.
     * Default is 0 (all players). Players below this level are silently ignored.
     */
    public static final ConfigKey<Integer> TRIGGER_PERMISSION_LEVEL = new ConfigKey<>(
            "triggerPermissionLevel", Integer.class, 0,
            "Required permission level for using the trigger word in chat (0-4). Default: 0 (all players). Players below this level are ignored.",
            v -> v >= 0 && v <= 4,
            v -> {
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("must be an integer between 0 and 4");
                }
            },
            List.of("0", "1", "2", "3", "4")
    );

    // ---- Ordered registry ----------------------------------------------------

    /**
     * Ordered list of all config keys.
     * <p>
     * The order determines:
     * <ul>
     *   <li>The order of entries in the JSON file</li>
     *   <li>The order of fields in the {@code /easyspec info} display</li>
     *   <li>The iteration order for tab-completion suggestions</li>
     * </ul>
     * Add new keys in the position you want them to appear.
     */
    public static final List<ConfigKey<?>> ALL = List.of(
            LANGUAGE,
            TRIGGER,
            HIDE_TRIGGER,
            PERMISSION_LEVEL,
            TRIGGER_PERMISSION_LEVEL
    );

    // ---- Lookup --------------------------------------------------------------

    /**
     * Find a config key by its JSON key name (e.g. {@code "language"}).
     *
     * @param name the JSON key name to search for
     * @return the matching {@code ConfigKey}, or {@link Optional#empty()} if not found
     */
    public static @NotNull Optional<ConfigKey<?>> byName(@NotNull String name) {
        for (ConfigKey<?> k : ALL) {
            if (k.getKey().equals(name)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }

    private ConfigKeys() {
    }
}

package lbb.easyspec.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central manager for mod configuration — the single entry point for all
 * config operations.
 * <p>
 * Replaces the old monolithic {@code Config.java} with a modular design:
 * <ul>
 *   <li>{@link ConfigKey} — describes one config option</li>
 *   <li>{@link ConfigKeys} — registry of all options</li>
 *   <li>{@link ConfigData} — immutable value snapshot</li>
 *   <li>{@code ConfigManager} — orchestrates load/save/validate/reset</li>
 * </ul>
 * <p>
 * <strong>Lifecycle:</strong>
 * <pre>{@code
 *   // 1. Initialize once during mod init
 *   ConfigManager.getInstance().initialize(FabricLoader.getInstance().getConfigDir());
 *
 *   // 2. Read values (type-safe)
 *   String lang = ConfigManager.getInstance().get(ConfigKeys.LANGUAGE);
 *   int level  = ConfigManager.getInstance().get(ConfigKeys.PERMISSION_LEVEL);
 *
 *   // 3. Write values (validates + persists)
 *   ConfigManager.getInstance().set(ConfigKeys.LANGUAGE, "zh_cn");
 *
 *   // 4. Reset
 *   ConfigManager.getInstance().reset(ConfigKeys.TRIGGER);  // single
 *   ConfigManager.getInstance().reset();                    // all
 *
 *   // 5. Reload from disk
 *   ConfigManager.getInstance().reload();
 * }</pre>
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("easyspec-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "easyspec.json";

    private static final ConfigManager INSTANCE = new ConfigManager();

    private @Nullable Path configFile;
    private @NotNull ConfigData data = ConfigData.defaults();

    private ConfigManager() {
    }

    // ---- Singleton access ----------------------------------------------------

    /**
     * Returns the singleton {@code ConfigManager} instance.
     * Call {@link #initialize(Path)} before any other method.
     */
    public static @NotNull ConfigManager getInstance() {
        return INSTANCE;
    }

    // ---- Initialization ------------------------------------------------------

    /**
     * One-time initialization. Must be called during mod startup before any
     * other config access. Loads the config file from disk, creating it with
     * defaults if it doesn't exist.
     *
     * @param configDir the Minecraft config directory
     *                  (from {@link FabricLoader#getConfigDir()})
     */
    public void initialize(@NotNull Path configDir) {
        this.configFile = configDir.resolve(FILE_NAME);
        loadFromDisk();
    }

    // ---- Type-safe read ------------------------------------------------------

    /**
     * Read a config value with full type safety.
     *
     * @param key the config key to read
     * @param <T> the value type
     * @return the current value, or the key's default if not set
     */
    public <T> @NotNull T get(@NotNull ConfigKey<T> key) {
        return data.get(key);
    }

    // ---- Type-safe write -----------------------------------------------------

    /**
     * Set a config value with full type safety. Validates the value via the
     * key's validator, then persists to disk.
     *
     * @param key   the config key to update
     * @param value the new value
     * @param <T>   the value type
     * @throws IllegalArgumentException if the value fails validation
     */
    public <T> void set(@NotNull ConfigKey<T> key, @NotNull T value) {
        if (!key.isValid(value)) {
            throw new IllegalArgumentException(
                    Messages.get("set_error_invalid_value").formatted(key.getKey(), value));
        }
        data = data.with(key, value);
        saveToDisk();
        LOGGER.info("EasySpec config option '{}' set to '{}'", key.getKey(), value);
    }

    // ---- String-based write (for command interface) --------------------------

    /**
     * Set a config value by key name and string value. Convenience method for
     * command handlers that receive raw string input.
     * <p>
     * Looks up the key, parses the string via {@link ConfigKey#parse(String)},
     * validates, and saves.
     *
     * @param keyName  the JSON key name (e.g. "language")
     * @param valueStr the value as a string (e.g. "zh_cn")
     * @throws IllegalArgumentException if the key is unknown or the value is invalid
     */
    public void set(@NotNull String keyName, @NotNull String valueStr) {
        ConfigKey<?> key = ConfigKeys.byName(keyName)
                .orElseThrow(() -> new IllegalArgumentException(
                        Messages.get("set_error_invalid_key").formatted(keyName)));

        Object value;
        try {
            value = key.parse(valueStr);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    Messages.get("set_error_invalid_value").formatted(keyName, valueStr));
        }

        // Delegate to type-safe set via unchecked cast (safe — parse() returns
        // the correct type by definition)
        setUnchecked(key, value);
    }

    @SuppressWarnings("unchecked")
    private <T> void setUnchecked(ConfigKey<T> key, Object value) {
        set(key, (T) value);
    }

    // ---- Reset ---------------------------------------------------------------

    /**
     * Reset all config values to their defaults and persist to disk.
     */
    public void reset() {
        data = ConfigData.defaults();
        saveToDisk();
        LOGGER.info("EasySpec config reset to defaults");
    }

    /**
     * Reset a single config value to its default and persist to disk.
     *
     * @param key the config key to reset
     */
    public void reset(@NotNull ConfigKey<?> key) {
        data = data.withUnchecked(key, key.getDefaultValue());
        saveToDisk();
        LOGGER.info("EasySpec config option '{}' reset to default", key.getKey());
    }

    /**
     * Reset a single config value by key name. Convenience method for command
     * handlers that receive raw string input.
     *
     * @param keyName the JSON key name (e.g. "trigger")
     * @throws IllegalArgumentException if the key is unknown
     */
    public void reset(@NotNull String keyName) {
        ConfigKey<?> key = ConfigKeys.byName(keyName)
                .orElseThrow(() -> new IllegalArgumentException(
                        Messages.get("set_error_invalid_key").formatted(keyName)));
        reset(key);
    }

    // ---- Reload --------------------------------------------------------------

    /**
     * Reload config from disk, discarding any unsaved changes.
     * Used by the {@code /easyspec reload} command.
     */
    public void reload() {
        if (configFile == null) {
            LOGGER.warn("ConfigManager not initialized — cannot reload");
            return;
        }
        loadFromDisk();
        LOGGER.info("EasySpec config reloaded from disk");
    }

    // ---- Data access ---------------------------------------------------------

    /**
     * Get a read-only snapshot of the current config data.
     * Used by the {@code /easyspec info} command to iterate all values.
     */
    public @NotNull ConfigData getData() {
        return data;
    }

    // ---- File I/O ------------------------------------------------------------

    /**
     * Load config from disk. On any error, uses defaults and triggers a save
     * so the file is always valid after this method returns.
     */
    private void loadFromDisk() {
        if (configFile == null) {
            LOGGER.warn("Config file path not set — using defaults");
            data = ConfigData.defaults();
            return;
        }

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                data = deserialize(json);
            } catch (Exception e) {
                LOGGER.error("Failed to load config from {}, using defaults", configFile, e);
                data = ConfigData.defaults();
                saveToDisk();
            }
        } else {
            data = ConfigData.defaults();
            saveToDisk();
        }

        logCurrentState();
    }

    /**
     * Save current config to disk. Creates parent directories if needed.
     * I/O errors are logged but not re-thrown — the mod keeps running.
     */
    private void saveToDisk() {
        if (configFile == null) {
            LOGGER.warn("Config file path not set — cannot save");
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, serialize());
            LOGGER.info("Saved config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", configFile, e);
        }
    }

    private void logCurrentState() {
        LOGGER.info("EasySpec config loaded: language={}, trigger=!{}, hideTrigger={}, permissionLevel={}, triggerPermissionLevel={}",
                data.get(ConfigKeys.LANGUAGE),
                data.get(ConfigKeys.TRIGGER),
                data.get(ConfigKeys.HIDE_TRIGGER),
                data.get(ConfigKeys.PERMISSION_LEVEL),
                data.get(ConfigKeys.TRIGGER_PERMISSION_LEVEL));
    }

    // ---- Serialization -------------------------------------------------------

    /**
     * Serialize current config to pretty-printed JSON with comment fields.
     * Iterates {@link ConfigKeys#ALL} so key order and comment numbering are
     * deterministic. Adding a new key automatically includes it in the output.
     */
    private @NotNull String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        List<ConfigKey<?>> keys = ConfigKeys.ALL;
        for (int i = 0; i < keys.size(); i++) {
            ConfigKey<?> key = keys.get(i);
            // Comment line
            sb.append("  \"_comment").append(i + 1).append("\": ");
            sb.append(GSON.toJson(key.getComment()));
            sb.append(",\n");
            // Value line
            sb.append("  \"").append(key.getKey()).append("\": ");
            sb.append(toJsonLiteral(data.get(key)));
            if (i < keys.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Deserialize a JSON string into a {@link ConfigData}, repairing any missing
     * or invalid fields along the way. If repairs are made, the file is re-saved.
     */
    private ConfigData deserialize(@NotNull String json) {
        JsonObject raw = null;

        // Try to parse JSON. On failure (empty file, corrupted syntax, etc.),
        // fall back to defaults and overwrite the file so it's valid next time.
        try {
            raw = GSON.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            LOGGER.warn("Malformed config JSON, using defaults", e);
        }

        if (raw == null) {
            ConfigData defaults = ConfigData.defaults();
            this.data = defaults;
            saveToDisk();
            return defaults;
        }

        Map<String, Object> values = new HashMap<>();
        boolean repaired = false;

        for (ConfigKey<?> key : ConfigKeys.ALL) {
            String keyName = key.getKey();

            if (raw.has(keyName)) {
                try {
                    Object parsed = parseJsonPrimitive(raw.get(keyName), key);
                    if (parsed != null && isValidFor(parsed, key)) {
                        values.put(keyName, parsed);
                    } else {
                        LOGGER.warn("Invalid value for config key '{}', using default", keyName);
                        values.put(keyName, key.getDefaultValue());
                        repaired = true;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse config key '{}', using default ({})",
                            keyName, e.getMessage());
                    values.put(keyName, key.getDefaultValue());
                    repaired = true;
                }
            } else {
                // Key missing from JSON — use default
                values.put(keyName, key.getDefaultValue());
                repaired = true;
            }
        }

        ConfigData result = ConfigData.fromMap(values);

        // Re-save if we repaired anything so the file stays in sync
        if (repaired) {
            this.data = result; // temporarily swap so serialize() sees the repaired data
            saveToDisk();
        }

        return result;
    }

    // ---- JSON helpers --------------------------------------------------------

    /**
     * Parse a JSON element into the Java type expected by the given key.
     */
    @SuppressWarnings("unchecked")
    private static <T> @Nullable T parseJsonPrimitive(@NotNull JsonElement element,
                                                       @NotNull ConfigKey<T> key) {
        Class<T> type = key.getType();
        if (type == String.class && element.isJsonPrimitive()) {
            return (T) element.getAsString();
        } else if (type == Boolean.class && element.isJsonPrimitive()) {
            return (T) (Boolean) element.getAsBoolean();
        } else if (type == Integer.class && element.isJsonPrimitive()) {
            try {
                return (T) (Integer) element.getAsInt();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Check whether a parsed value passes the key's validator.
     */
    @SuppressWarnings("unchecked")
    private static <T> boolean isValidFor(@NotNull Object value, @NotNull ConfigKey<T> key) {
        if (!key.getType().isInstance(value)) {
            return false;
        }
        return key.isValid((T) value);
    }

    /**
     * Convert a config value to its JSON literal representation.
     */
    private static String toJsonLiteral(@NotNull Object value) {
        if (value instanceof String s) {
            return GSON.toJson(s);
        } else if (value instanceof Boolean b) {
            return b.toString();
        } else if (value instanceof Integer i) {
            return i.toString();
        }
        return GSON.toJson(value);
    }
}

package lbb.easyspec.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("easyspec-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String language = "en_us";
    private String trigger = "s";
    private boolean hideTrigger = false;
    private int permissionLevel = 2;
    private int triggerPermissionLevel = 0;

    private static Config instance;

    public static Config getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public String getLanguage() {
        return language;
    }

    public String getTrigger() {
        return trigger;
    }

    public boolean isHideTrigger() {
        return hideTrigger;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public int getTriggerPermissionLevel() {
        return triggerPermissionLevel;
    }

    public static Config load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("easyspec.json");

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);

                // Check raw JSON for missing fields (Gson may use constructor defaults, hiding absences)
                JsonObject raw = GSON.fromJson(json, JsonObject.class);
                boolean repaired = raw == null || !raw.has("language");
                if (raw == null || !raw.has("trigger")) repaired = true;
                if (raw == null || !raw.has("hideTrigger")) repaired = true;
                if (raw == null || !raw.has("permissionLevel")) repaired = true;
                if (raw == null || !raw.has("triggerPermissionLevel")) repaired = true;
                if (raw == null || !raw.has("_comment1")) repaired = true;
                if (raw == null || !raw.has("_comment2")) repaired = true;
                if (raw == null || !raw.has("_comment3")) repaired = true;
                if (raw == null || !raw.has("_comment4")) repaired = true;
                if (raw == null || !raw.has("_comment5")) repaired = true;

                instance = GSON.fromJson(json, Config.class);
                if (instance == null) {
                    instance = new Config();
                    repaired = true;
                }

                // Fill null fields as safety net (in case Gson bypassed the constructor)
                if (instance.language == null) {
                    instance.language = "en_us";
                    repaired = true;
                }
                if (instance.trigger == null) {
                    instance.trigger = "s";
                    repaired = true;
                }
                // hideTrigger missing from JSON — Gson leaves primitive boolean as false, which is the desired default
                if (raw != null && !raw.has("hideTrigger")) {
                    instance.hideTrigger = false;
                    repaired = true;
                }
                // triggerPermissionLevel missing from JSON
                if (raw != null && !raw.has("triggerPermissionLevel")) {
                    instance.triggerPermissionLevel = 0;
                    repaired = true;
                }

                // Validate language
                if (!Messages.SUPPORTED_LANGUAGES.contains(instance.language)) {
                    LOGGER.warn("Unsupported language '{}' in config, resetting to 'en_us'", instance.language);
                    instance.language = "en_us";
                    repaired = true;
                }

                // Validate trigger (must not be empty)
                if (instance.trigger.isBlank()) {
                    LOGGER.warn("Invalid trigger in config, resetting to default 's'");
                    instance.trigger = "s";
                    repaired = true;
                } else {
                    instance.trigger = instance.trigger.trim();
                }

                // Validate permissionLevel (must be 0-4)
                if (instance.permissionLevel < 0 || instance.permissionLevel > 4) {
                    LOGGER.warn("Invalid permissionLevel '{}' in config, resetting to default 2", instance.permissionLevel);
                    instance.permissionLevel = 2;
                    repaired = true;
                }

                // Validate triggerPermissionLevel (must be 0-4)
                if (instance.triggerPermissionLevel < 0 || instance.triggerPermissionLevel > 4) {
                    LOGGER.warn("Invalid triggerPermissionLevel '{}' in config, resetting to default 0", instance.triggerPermissionLevel);
                    instance.triggerPermissionLevel = 0;
                    repaired = true;
                }

                // Re-save with comments if anything was missing or invalid
                if (repaired) {
                    saveConfig(configFile);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
                instance = new Config();
            }
        } else {
            instance = new Config();
            saveConfig(configFile);
        }

        LOGGER.info("EasySpec config loaded: language={}, trigger=!{}, hideTrigger={}, permissionLevel={}, triggerPermissionLevel={}", instance.language, instance.trigger, instance.hideTrigger, instance.permissionLevel, instance.triggerPermissionLevel);
        return instance;
    }

    /**
     * Reset the config to default values, overwrite the file on disk, and
     * clear the messages cache so the next lookup uses the new language.
     */
    public static void reset() {
        instance = new Config();
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("easyspec.json");
        saveConfig(configFile);
        LOGGER.info("EasySpec config reset to defaults");
    }

    /**
     * Reset a single config option to its default value by key.
     *
     * @param key the config key to reset (language, trigger, hideTrigger)
     * @throws IllegalArgumentException if the key is unknown
     */
    public static void reset(@NotNull String key) throws IllegalArgumentException {
        Config config = getInstance();

        switch (key) {
            case "language" -> config.language = "en_us";
            case "trigger" -> config.trigger = "s";
            case "hideTrigger" -> config.hideTrigger = false;
            case "permissionLevel" -> config.permissionLevel = 2;
            case "triggerPermissionLevel" -> config.triggerPermissionLevel = 0;
            default ->
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_key").formatted(key));
        }

        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("easyspec.json");
        saveConfig(configFile);
        LOGGER.info("EasySpec config option '{}' reset to default", key);
    }

    /**
     * Set a config option by key. Validates the value, updates the instance, and saves to disk.
     *
     * @param key   the config key (language, trigger, hideTrigger)
     * @param value the value to set (as a string; booleans accept "true"/"false")
     * @throws IllegalArgumentException if the key is unknown or the value is invalid
     */
    public static void set(@NotNull String key, @NotNull String value) throws IllegalArgumentException {
        Config config = getInstance();

        switch (key) {
            case "language" -> {
                if (!Messages.SUPPORTED_LANGUAGES.contains(value)) {
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                }
                config.language = value;
            }
            case "trigger" -> {
                if (value.isBlank()) {
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                }
                config.trigger = value.trim();
            }
            case "hideTrigger" -> {
                if ("true".equalsIgnoreCase(value)) {
                    config.hideTrigger = true;
                } else if ("false".equalsIgnoreCase(value)) {
                    config.hideTrigger = false;
                } else {
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                }
            }
            case "permissionLevel" -> {
                try {
                    int level = Integer.parseInt(value);
                    if (level < 0 || level > 4) {
                        throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                    }
                    config.permissionLevel = level;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                }
            }
            case "triggerPermissionLevel" -> {
                try {
                    int level = Integer.parseInt(value);
                    if (level < 0 || level > 4) {
                        throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                    }
                    config.triggerPermissionLevel = level;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_value").formatted(key, value));
                }
            }
            default ->
                    throw new IllegalArgumentException(Messages.get("set_error_invalid_key").formatted(key));
        }

        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("easyspec.json");
        saveConfig(configFile);
        LOGGER.info("EasySpec config option '{}' set to '{}'", key, value);
    }

    /**
     * Save config with human-readable comments, using the current instance values.
     * Used for both first-time creation and repairing missing/invalid fields.
     */
    private static void saveConfig(@NotNull Path configFile) {
        try {
            Files.createDirectories(configFile.getParent());
            String json =
                    """
                            {
                              "_comment1": "Language: en_us, zh_cn, ja_jp, ko_kr, fr_fr, de_de, es_es, ru_ru, pt_br",
                              "language": "%s",
                              "_comment2": "Trigger word: type '!' + this in chat to toggle spectator. Default: s (i.e. !s)",
                              "trigger": "%s",
                              "_comment3": "Hide the trigger message from chat (default: false). Set true to hide it so other players don't see it in chat.",
                              "hideTrigger": %s,
                              "_comment4": "Required permission level for /easyspec commands (0-4). Default: 2 (operator). Set 0 for all players.",
                              "permissionLevel": %d,
                              "_comment5": "Required permission level for using the trigger word in chat (0-4). Default: 0 (all players). Players below this level are ignored.",
                              "triggerPermissionLevel": %d
                            }
                            """.formatted(instance.language, instance.trigger, instance.hideTrigger, instance.permissionLevel, instance.triggerPermissionLevel);
            Files.writeString(configFile, json);
            LOGGER.info("Saved config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}

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
                if (raw == null || !raw.has("_comment1")) repaired = true;
                if (raw == null || !raw.has("_comment2")) repaired = true;

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

        LOGGER.info("EasySpec config loaded: language={}, trigger=!{}", instance.language, instance.trigger);
        return instance;
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
                              "_comment2": "Trigger word: type '!' + this in chat to toggle spectator. Default: s (i.e. !s)",
                              "language": "%s",
                              "trigger": "%s"
                            }
                            """.formatted(instance.language, instance.trigger);
            Files.writeString(configFile, json);
            LOGGER.info("Saved config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}

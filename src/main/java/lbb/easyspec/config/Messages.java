package lbb.easyspec.config;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads translations from the JSON language files in assets/easyspec/lang/.
 * The JSON files are the single source of truth — no hardcoded strings here.
 */
public class Messages {
    private static final Logger LOGGER = LoggerFactory.getLogger("easyspec-messages");
    private static final Gson GSON = new Gson();

    private static String currentLang = null;
    private static Map<String, String> currentMessages = null;

    /**
     * Forces the messages cache to be cleared so the next call to {@link #get(String)}
     * re-reads the language file from disk/assets.
     * Used by the /easyspec reload command to pick up config changes.
     */
    public static void reload() {
        currentLang = null;
        currentMessages = null;
    }

    public static String get(String key) {
        String lang = ConfigManager.getInstance().get(ConfigKeys.LANGUAGE);
        if (!lang.equals(currentLang) || currentMessages == null) {
            load(lang);
        }
        // If loading failed, fall back to en_us
        if (currentMessages == null && !lang.equals("en_us")) {
            load("en_us");
        }
        if (currentMessages != null) {
            return currentMessages.getOrDefault(key, key);
        }
        return key;
    }

    private static void load(String lang) {
        currentLang = lang;
        currentMessages = null;

        String path = "/assets/easyspec/lang/" + lang + ".json";
        try (InputStream in = Messages.class.getResourceAsStream(path)) {
            if (in == null) {
                LOGGER.warn("Language file not found: {}", path);
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = GSON.fromJson(new InputStreamReader(in), Map.class);
            if (raw == null) {
                LOGGER.warn("Empty language file: {}", path);
                return;
            }
            Map<String, String> messages = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey.startsWith("message.easyspec.")) {
                    String shortKey = entryKey.substring("message.easyspec.".length());
                    messages.put(shortKey, entry.getValue().toString());
                }
            }
            currentMessages = messages;
        } catch (Exception e) {
            LOGGER.error("Failed to load language file: {}", path, e);
        }
    }
}

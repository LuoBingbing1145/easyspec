package lbb.easyspec.config;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Loads translations from the JSON language files in assets/easyspec/lang/.
 * The JSON files are the single source of truth — no hardcoded strings here.
 */
public class Messages {
    private static final Logger LOGGER = LoggerFactory.getLogger("easyspec-messages");
    private static final Gson GSON = new Gson();

    public static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "zh_cn", "en_us", "ja_jp", "ko_kr", "fr_fr", "de_de", "es_es", "ru_ru", "pt_br"
    );

    private static String currentLang = null;
    private static Map<String, String> currentMessages = null;

    public static String get(String key) {
        String lang = Config.getInstance().getLanguage();
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

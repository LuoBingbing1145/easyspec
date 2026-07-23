package lbb.easyspec.config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of all configuration values.
 * <p>
 * Access is type-safe via {@link #get(ConfigKey)}. Updates produce a new
 * instance via {@link #with(ConfigKey, Object)} or {@link #withUnchecked(ConfigKey, Object)},
 * leaving the original unchanged.
 * <p>
 * Obtain instances through:
 * <ul>
 *   <li>{@link #defaults()} — all values set to their defaults</li>
 *   <li>{@link ConfigManager#getData()} — current live config snapshot</li>
 * </ul>
 */
public final class ConfigData {

    private final Map<String, Object> values;

    private ConfigData(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    // ---- Factory methods -----------------------------------------------------

    /**
     * Create a {@code ConfigData} with every key set to its default value.
     */
    public static @NotNull ConfigData defaults() {
        Map<String, Object> values = new HashMap<>();
        for (ConfigKey<?> key : ConfigKeys.ALL) {
            values.put(key.getKey(), key.getDefaultValue());
        }
        return new ConfigData(values);
    }

    /**
     * Create from a raw map (used during deserialization).
     */
    static @NotNull ConfigData fromMap(@NotNull Map<String, Object> values) {
        return new ConfigData(values);
    }

    // ---- Type-safe access ----------------------------------------------------

    /**
     * Get a typed config value. Returns the key's default if no value has been
     * explicitly set or if the stored value is not of the expected type.
     *
     * @param key the config key to look up
     * @param <T> the value type
     * @return the current value, or the key's default
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull T get(@NotNull ConfigKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = values.get(key.getKey());
        if (key.getType().isInstance(value)) {
            return (T) value;
        }
        return key.getDefaultValue();
    }

    /**
     * Create a new {@code ConfigData} with one value changed.
     * The original instance is not modified.
     *
     * @param key   the config key to update
     * @param value the new value (must not be null)
     * @param <T>   the value type
     * @return a new {@code ConfigData} with the updated value
     */
    public <T> @NotNull ConfigData with(@NotNull ConfigKey<T> key, @NotNull T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Map<String, Object> newValues = new HashMap<>(this.values);
        newValues.put(key.getKey(), value);
        return new ConfigData(newValues);
    }

    /**
     * Like {@link #with} but accepts a wildcard key ({@code ConfigKey<?>}) and
     * performs an unchecked cast internally. Safe because the value comes from
     * the key's own {@link ConfigKey#getDefaultValue()}.
     *
     * @param key   the config key to reset
     * @param value the value to set (must match the key's type at runtime)
     * @return a new {@code ConfigData} with the updated value
     */
    @Contract("_, _ -> new")
    @NotNull ConfigData withUnchecked(@NotNull ConfigKey<?> key, @NotNull Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Map<String, Object> newValues = new HashMap<>(this.values);
        newValues.put(key.getKey(), value);
        return new ConfigData(newValues);
    }

    /**
     * Expose the raw map for serialization. The returned map is unmodifiable.
     */
    Map<String, Object> asMap() {
        return values;
    }

    // ---- Object contract -----------------------------------------------------

    @Override
    public String toString() {
        return "ConfigData" + values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigData that)) return false;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}

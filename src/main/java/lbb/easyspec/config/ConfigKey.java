package lbb.easyspec.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Immutable descriptor for a single configuration option.
 * <p>
 * Each {@code ConfigKey} carries everything needed to define, validate, parse,
 * and document one config entry: its JSON key name, Java type, default value,
 * human-readable comment, validation rule, string parser (for command input),
 * and tab-completion suggestions.
 * <p>
 * Adding a new config option means creating one new {@code ConfigKey} constant
 * in {@link ConfigKeys} and adding it to the {@code ALL} list — nothing else
 * needs to change. Serialization, validation, command suggestions, and info
 * display all pick it up automatically.
 *
 * @param <T> the Java type of the config value (String, Boolean, Integer, etc.)
 */
public final class ConfigKey<T> {

    private final @NotNull String key;
    private final @NotNull Class<T> type;
    private final @NotNull T defaultValue;
    private final @NotNull String comment;
    private final @Nullable Predicate<T> validator;
    private final @NotNull Function<String, T> parser;
    private final @NotNull List<String> suggestions;

    /**
     * @param key          JSON key name in the config file (e.g. "language")
     * @param type         Java class of the value (e.g. {@code String.class})
     * @param defaultValue fallback value used when the key is missing or invalid
     * @param comment      human-readable description, written as a {@code _commentN}
     *                     entry in the JSON file
     * @param validator    optional predicate; return {@code false} to reject a value.
     *                     Pass {@code null} to accept any non-null value of the correct type.
     * @param parser       converts a string (from command input) to a typed value.
     *                     Should throw {@link IllegalArgumentException} on invalid input.
     * @param suggestions  tab-completion values for commands (empty list = free-text input)
     */
    public ConfigKey(@NotNull String key,
                     @NotNull Class<T> type,
                     @NotNull T defaultValue,
                     @NotNull String comment,
                     @Nullable Predicate<T> validator,
                     @NotNull Function<String, T> parser,
                     @NotNull List<String> suggestions) {
        this.key = Objects.requireNonNull(key, "key");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.comment = Objects.requireNonNull(comment, "comment");
        this.validator = validator;
        this.parser = Objects.requireNonNull(parser, "parser");
        this.suggestions = List.copyOf(suggestions);
    }

    // ---- Accessors -----------------------------------------------------------

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull Class<T> getType() {
        return type;
    }

    public @NotNull T getDefaultValue() {
        return defaultValue;
    }

    public @NotNull String getComment() {
        return comment;
    }

    public @NotNull List<String> getSuggestions() {
        return suggestions;
    }

    // ---- Behaviour -----------------------------------------------------------

    /**
     * Returns {@code true} if the value passes validation.
     * If no validator is set, any non-null value of the correct type is accepted.
     */
    public boolean isValid(@NotNull T value) {
        if (validator == null) {
            return true;
        }
        return validator.test(value);
    }

    /**
     * Parses a string from command input into the typed value.
     * Throws {@link IllegalArgumentException} on invalid input.
     */
    public @NotNull T parse(@NotNull String input) {
        return parser.apply(input);
    }

    // ---- Object contract -----------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigKey<?> that)) return false;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "ConfigKey{" + key + "}";
    }
}

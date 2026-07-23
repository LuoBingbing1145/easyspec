package lbb.easyspec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lbb.easyspec.EasySpec;
import lbb.easyspec.config.ConfigKey;
import lbb.easyspec.config.ConfigKeys;
import lbb.easyspec.config.ConfigManager;
import lbb.easyspec.config.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Registers the /easyspec command tree.
 * <p>
 * Commands:
 *   /easyspec reload       — Reloads config/easyspec.json and re-initializes translations.
 *   /easyspec reset        — Resets config/easyspec.json to default values.
 *   /easyspec reset {key}  — Resets a single config option to its default value.
 *   /easyspec set {key} {value} — Sets a config option to the given value.
 *   /easyspec info         — Displays all current configuration values.
 * <p>
 * Permission check uses the {@code easyspec.command} permission node via
 * Fabric Permissions API (compatible with LuckPerms). When no permission mod
 * is installed, falls back to the vanilla operator level from config
 * ({@code permissionLevel}, default 2 = operator).
 */
public class EasySpecCommand {

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("easyspec")
                        .requires(source -> Permissions.check(source, EasySpec.PERM_COMMAND, ConfigManager.getInstance().get(ConfigKeys.PERMISSION_LEVEL)))
                        .then(Commands.literal("reload")
                                .executes(EasySpecCommand::reloadConfig)
                        )
                        .then(Commands.literal("reset")
                                .executes(EasySpecCommand::resetConfig)
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(EasySpecCommand::suggestKeys)
                                        .executes(EasySpecCommand::resetConfigByKey)
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(EasySpecCommand::suggestKeys)
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .suggests(EasySpecCommand::suggestValues)
                                                .executes(EasySpecCommand::setConfig)
                                        )
                                )
                        )
                        .then(Commands.literal("info")
                                .executes(EasySpecCommand::showInfo)
                        )
        );
    }

    private static int reloadConfig(@NotNull CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.getInstance().reload();
            Messages.reload();
            context.getSource().sendSuccess(
                    () -> Component.literal("§a[EasySpec] " + Messages.get("reloaded")),
                    true
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("§c[EasySpec] Failed to reload config: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int resetConfig(@NotNull CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.getInstance().reset();
            Messages.reload();
            context.getSource().sendSuccess(
                    () -> Component.literal("§a[EasySpec] " + Messages.get("reset")),
                    true
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("§c[EasySpec] Failed to reset config: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int resetConfigByKey(@NotNull CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");

        try {
            ConfigManager.getInstance().reset(key);
            Messages.reload();
            context.getSource().sendSuccess(
                    () -> Component.literal("§a[EasySpec] " + Messages.get("reset_key").formatted(key)),
                    true
            );
            warnIfLuckPermsOverrides(context, key);
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(
                    Component.literal("§c[EasySpec] " + e.getMessage())
            );
            return 0;
        }
    }

    private static int setConfig(@NotNull CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");

        try {
            ConfigManager.getInstance().set(key, value);
            Messages.reload();
            context.getSource().sendSuccess(
                    () -> Component.literal("§a[EasySpec] " + Messages.get("set_success").formatted(key, value)),
                    true
            );
            warnIfLuckPermsOverrides(context, key);
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(
                    Component.literal("§c[EasySpec] " + e.getMessage())
            );
            return 0;
        }
    }

    private static int showInfo(@NotNull CommandContext<CommandSourceStack> context) {
        // Iterate ConfigKeys.ALL so adding a new key automatically includes it in the info display.
        Object[] values = ConfigKeys.ALL.stream()
                .map(k -> String.valueOf(ConfigManager.getInstance().getData().get(k)))
                .toArray();

        context.getSource().sendSuccess(
                () -> Component.literal("§a[EasySpec] " + Messages.get("info").formatted(values)),
                false
        );

        // When LuckPerms is installed, the config permission levels are ignored —
        // LP manages these via permission nodes instead. Tell the admin what
        // nodes are actually effective.
        if (EasySpec.isLuckPermsLoaded()) {
            context.getSource().sendSystemMessage(
                    Component.literal("§e[EasySpec] ⚠ " + Messages.get("lp_info"))
            );
        }

        return 1;
    }

    /**
     * If LuckPerms is installed and the config key being modified is
     * {@code permissionLevel} or {@code triggerPermissionLevel}, send a
     * warning to the command source that these values have no effect
     * while LP is active.
     */
    private static void warnIfLuckPermsOverrides(CommandContext<CommandSourceStack> context, String key) {
        if (!EasySpec.isLuckPermsLoaded()) return;

        String warningKey = switch (key) {
            case "permissionLevel" -> "lp_warning_command";
            case "triggerPermissionLevel" -> "lp_warning_trigger";
            default -> null;
        };

        if (warningKey != null) {
            context.getSource().sendSystemMessage(
                    Component.literal("§e[EasySpec] ⚠ " + Messages.get(warningKey))
            );
        }
    }

    /**
     * Suggest available config keys for the <code>key</code> argument.
     * Iterates {@link ConfigKeys#ALL} so newly added keys appear automatically.
     */
    private static @NotNull CompletableFuture<Suggestions> suggestKeys(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        List<String> keyNames = ConfigKeys.ALL.stream()
                .map(ConfigKey::getKey)
                .toList();
        return SharedSuggestionProvider.suggest(keyNames, builder);
    }

    /**
     * Suggest valid values for the {@code value} argument based on the chosen key.
     * Uses each key's {@link ConfigKey#getSuggestions()} — returns an empty
     * future if the key has no suggestions (free-text input) or is unknown.
     */
    private static @NotNull CompletableFuture<Suggestions> suggestValues(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        String keyName = StringArgumentType.getString(context, "key");
        return ConfigKeys.byName(keyName)
                .map(k -> {
                    List<String> suggestions = k.getSuggestions();
                    if (suggestions.isEmpty()) {
                        return builder.buildFuture();
                    }
                    return SharedSuggestionProvider.suggest(suggestions, builder);
                })
                .orElse(builder.buildFuture());
    }
}

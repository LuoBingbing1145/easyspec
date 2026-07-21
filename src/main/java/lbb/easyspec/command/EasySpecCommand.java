package lbb.easyspec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lbb.easyspec.config.Config;
import lbb.easyspec.config.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Registers the /easyspec command tree.
 * <p>
 * Commands:
 *   /easyspec reload — Reloads config/easyspec.json and re-initializes translations.
 *   /easyspec reset  — Resets config/easyspec.json to default values.
 *                      Requires op level 2.
 */
public class EasySpecCommand {

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("easyspec")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(EasySpecCommand::reloadConfig)
                        )
                        .then(Commands.literal("reset")
                                .executes(EasySpecCommand::resetConfig)
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
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            Config.load();
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

    private static int resetConfig(CommandContext<CommandSourceStack> context) {
        try {
            Config.reset();
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

    private static int setConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");

        try {
            Config.set(key, value);
            Messages.reload();
            context.getSource().sendSuccess(
                    () -> Component.literal("§a[EasySpec] " + Messages.get("set_success").formatted(key, value)),
                    true
            );
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(
                    Component.literal("§c[EasySpec] " + e.getMessage())
            );
            return 0;
        }
    }

    /**
     * Suggest available config keys for the <code>key</code> argument.
     */
    private static @NotNull CompletableFuture<Suggestions> suggestKeys(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
                new String[]{"language", "trigger", "hideTrigger"},
                builder
        );
    }

    /**
     * Suggest valid values for the <code>value</code> argument based on the chosen key.
     * <ul>
     *   <li>language → supported language codes</li>
     *   <li>hideTrigger → true / false</li>
     *   <li>trigger → free text, no suggestions</li>
     * </ul>
     */
    private static CompletableFuture<Suggestions> suggestValues(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        String key = StringArgumentType.getString(context, "key");
        return switch (key) {
            case "language" -> SharedSuggestionProvider.suggest(
                    Messages.SUPPORTED_LANGUAGES, builder
            );
            case "hideTrigger" -> SharedSuggestionProvider.suggest(
                    new String[]{"true", "false"}, builder
            );
            default -> builder.buildFuture();
        };
    }
}

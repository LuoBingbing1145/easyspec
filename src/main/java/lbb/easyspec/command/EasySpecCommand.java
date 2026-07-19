package lbb.easyspec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import lbb.easyspec.config.Config;
import lbb.easyspec.config.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
}

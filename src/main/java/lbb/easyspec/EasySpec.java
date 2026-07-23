package lbb.easyspec;

import lbb.easyspec.command.EasySpecCommand;
import lbb.easyspec.config.Config;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasySpec implements ModInitializer {
    public static final String MOD_ID = "easyspec";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Permission node for accessing {@code /easyspec} commands. */
    public static final String PERM_COMMAND = "easyspec.command";
    /** Permission node for using the {@code !s} trigger word in chat. */
    public static final String PERM_TRIGGER = "easyspec.trigger";

    /**
     * Check whether LuckPerms is loaded. When LP is active,
     * {@code permissionLevel} and {@code triggerPermissionLevel} in the config
     * file are ignored — LP manages these via permission nodes instead.
     */
    public static boolean isLuckPermsLoaded() {
        return FabricLoader.getInstance().isModLoaded("luckperms");
    }

    @Override
    public void onInitialize() {
        Config.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EasySpecCommand.register(dispatcher)
        );

        // Make LuckPerms (if installed) aware of permission nodes by firing
        // a check from the console source during server startup. This lets
        // LP auto-discover "easyspec.command" and "easyspec.trigger" so they
        // appear in the LP web editor without requiring manual entry.
        // Without LP, Permissions.check() is a no-op and has no side effects.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CommandSourceStack console = server.createCommandSourceStack();
            Permissions.check(console, PERM_COMMAND, Config.getInstance().getPermissionLevel());
            Permissions.check(console, PERM_TRIGGER, Config.getInstance().getTriggerPermissionLevel());
            LOGGER.info("Announced permission nodes: {}, {}", PERM_COMMAND, PERM_TRIGGER);
        });

        LOGGER.info("EasySpec initialized!");
    }

    @Contract("_ -> new")
    public static @NotNull ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}

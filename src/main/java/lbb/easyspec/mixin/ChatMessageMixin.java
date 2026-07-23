package lbb.easyspec.mixin;

import lbb.easyspec.EasySpec;
import lbb.easyspec.SpectatorManager;
import lbb.easyspec.config.Config;
import lbb.easyspec.config.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts chat messages to handle the spectator toggle trigger.
 * The trigger word is configurable via config/easyspec.json (default "!s").
 * When a player types the trigger in chat, it toggles spectator mode.
 * <p>
 * Permission check uses the {@code easyspec.trigger} permission node via
 * Fabric Permissions API (compatible with LuckPerms). When no permission mod
 * is installed, falls back to the vanilla operator level from config
 * ({@code triggerPermissionLevel}, default 0 = all players).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ChatMessageMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(@NotNull ServerboundChatPacket packet, CallbackInfo ci) {
        String message = packet.message().trim();
        String trigger = "!" + Config.getInstance().getTrigger();
        if (message.equalsIgnoreCase(trigger)) {
            if (player != null) {
                // Check permission via Fabric Permissions API (supports LuckPerms).
                // Falls back to vanilla operator level when no permission mod is installed.
                int requiredLevel = Config.getInstance().getTriggerPermissionLevel();
                if (!Permissions.check(player, EasySpec.PERM_TRIGGER, requiredLevel)) {
                    ServerPlayer p = player;
                    p.server.execute(() -> p.sendSystemMessage(Component.literal(
                            Messages.get("no_permission")
                    )));
                    // Cancel or broadcast the chat message based on hideTrigger
                    if (Config.getInstance().isHideTrigger()) {
                        ci.cancel();
                    }
                    return;
                }

                // Must run on server thread — handleChat is called from Netty IO thread
                // and teleport/gamemode changes require the main server thread
                ServerPlayer p = player;
                p.server.execute(() -> SpectatorManager.toggle(p));

                // Only cancel the chat message when hideTrigger is enabled
                if (Config.getInstance().isHideTrigger()) {
                    ci.cancel();
                }
            }
        }
    }
}

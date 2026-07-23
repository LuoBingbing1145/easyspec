package lbb.easyspec.mixin;

import lbb.easyspec.SpectatorManager;
import lbb.easyspec.config.ConfigKeys;
import lbb.easyspec.config.ConfigManager;
import lbb.easyspec.config.Messages;
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
 * When a player types the trigger in chat, it toggles spectator mode
 * and cancels the chat message so it's not broadcast to other players.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ChatMessageMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(@NotNull ServerboundChatPacket packet, CallbackInfo ci) {
        String message = packet.message().trim();
        String trigger = "!" + ConfigManager.getInstance().get(ConfigKeys.TRIGGER);
        if (message.equalsIgnoreCase(trigger)) {
            if (player != null) {
                // Check permission — if the player doesn't have the required level, notify them
                int requiredLevel = ConfigManager.getInstance().get(ConfigKeys.TRIGGER_PERMISSION_LEVEL);
                if (!player.hasPermissions(requiredLevel)) {
                    ServerPlayer p = player;
                    p.server.execute(() -> p.sendSystemMessage(Component.literal(
                            Messages.get("no_permission")
                    )));
                    // Cancel or broadcast the chat message based on hideTrigger
                    if (ConfigManager.getInstance().get(ConfigKeys.HIDE_TRIGGER)) {
                        ci.cancel();
                    }
                    return;
                }

                // Must run on server thread — handleChat is called from Netty IO thread
                // and teleport/gamemode changes require the main server thread
                ServerPlayer p = player;
                p.server.execute(() -> SpectatorManager.toggle(p));

                // Only cancel the chat message when hideTrigger is enabled
                if (ConfigManager.getInstance().get(ConfigKeys.HIDE_TRIGGER)) {
                    ci.cancel();
                }
            }
        }
    }
}

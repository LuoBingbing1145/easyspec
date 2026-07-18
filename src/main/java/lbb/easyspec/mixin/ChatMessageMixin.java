package lbb.easyspec.mixin;

import lbb.easyspec.SpectatorManager;
import lbb.easyspec.config.Config;
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
        String trigger = "!" + Config.getInstance().getTrigger();
        if (message.equalsIgnoreCase(trigger)) {
            if (player != null) {
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

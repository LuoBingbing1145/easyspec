package lbb.easyspec.mixin;

import lbb.easyspec.SpectatorManager;
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
 * Intercepts chat messages to handle the !s spectator toggle trigger.
 * When a player types "!s" in chat (without slash), it toggles spectator mode
 * and cancels the chat message so it's not broadcast to other players.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ChatMessageMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(@NotNull ServerboundChatPacket packet, CallbackInfo ci) {
        String message = packet.message().trim();
        if (message.equalsIgnoreCase("!s")) {
            if (player != null) {
                SpectatorManager.toggle(player);
                ci.cancel();
            }
        }
    }
}

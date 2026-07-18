package lbb.easyspec;

import lbb.easyspec.config.Config;
import lbb.easyspec.config.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spectator mode toggle for all players.
 * Tracks original game mode, position, and dimension so players can return.
 */
public class SpectatorManager {
    private static final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    /**
     * Toggle spectator mode for a player.
     * If not currently tracked, saves current state and switches to spectator.
     * If currently tracked, restores original state and removes tracking.
     */
    public static void toggle(@NotNull ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerState existing = states.remove(uuid);

        if (existing != null) {
            // Restore original game mode and position
            ServerLevel level = player.server.getLevel(existing.dimension);
            if (level != null) {
                player.setGameMode(existing.gameType);
                player.teleportTo(
                    level,
                    existing.x, existing.y, existing.z,
                    existing.yRot, existing.xRot
                );
            }
            player.sendSystemMessage(Component.literal(
                Messages.get("restored")
            ));
        } else {
            // Save current state and switch to spectator
            GameType currentMode = player.gameMode.getGameModeForPlayer();
            PlayerState state = new PlayerState(
                currentMode,
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.level().dimension()
            );
            states.put(uuid, state);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal(
                Messages.get("toggled").formatted(Config.getInstance().getTrigger())
            ));
        }
    }

    /**
     * Check if a player is currently being tracked (i.e. they used !s to enter spectator).
     */
    public static boolean isTracked(UUID uuid) {
        return states.containsKey(uuid);
    }

    private record PlayerState(GameType gameType, double x, double y, double z, float yRot, float xRot, ResourceKey<Level> dimension) {
    }
}

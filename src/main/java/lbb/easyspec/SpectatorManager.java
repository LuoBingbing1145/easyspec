package lbb.easyspec;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

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
    public static void toggle(ServerPlayer player) {
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
                "§a已返回原游戏模式并传送回原位"
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
                "§a已切换到旁观者模式，输入 !s 返回原位"
            ));
        }
    }

    /**
     * Check if a player is currently being tracked (i.e. they used !s to enter spectator).
     */
    public static boolean isTracked(UUID uuid) {
        return states.containsKey(uuid);
    }

    private static class PlayerState {
        final GameType gameType;
        final double x, y, z;
        final float yRot, xRot;
        final ResourceKey<Level> dimension;

        PlayerState(GameType gameType, double x, double y, double z,
                    float yRot, float xRot, ResourceKey<Level> dimension) {
            this.gameType = gameType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.dimension = dimension;
        }
    }
}

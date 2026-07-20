package lbb.easyspec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lbb.easyspec.config.Config;
import lbb.easyspec.config.Messages;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spectator mode toggle for all players.
 * Tracks original game mode, position, and dimension so players can return.
 * Player states are persisted per-world via {@link SavedData} so they survive restarts.
 */
public class SpectatorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("easyspec-spectator");

    /**
     * Toggle spectator mode for a player.
     * If not currently tracked, saves current state and switches to spectator.
     * If currently tracked, restores original state and removes tracking.
     */
    public static void toggle(@NotNull ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerStateStore store = PlayerStateStore.get(player.server);
        PlayerState existing = store.states.remove(uuid);

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
            store.states.put(uuid, state);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal(
                Messages.get("toggled").formatted(Config.getInstance().getTrigger())
            ));
        }

        store.setDirty();
    }

    /**
     * Check if a player is currently being tracked (i.e. they used !s to enter spectator).
     */
    public static boolean isTracked(MinecraftServer server, UUID uuid) {
        return PlayerStateStore.get(server).states.containsKey(uuid);
    }

    /**
     * Clear all saved states and mark for save.
     */
    public static void clearAll(MinecraftServer server) {
        PlayerStateStore store = PlayerStateStore.get(server);
        store.states.clear();
        store.setDirty();
        LOGGER.info("Cleared all saved player states");
    }

    private record PlayerState(GameType gameType, double x, double y, double z, float yRot, float xRot, ResourceKey<Level> dimension) {
    }

    /**
     * Per-world persistent storage for player states.
     * Data is saved via Minecraft's {@link SavedData} system to
     * {@code world/data/easyspec-player-states.dat}, surviving restarts
     * and being scoped correctly per save file.
     */
    private static class PlayerStateStore extends SavedData {
        private static final String DATA_NAME = "easyspec-player-states";
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        public final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

        public static @NotNull PlayerStateStore get(@NotNull MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(
                PlayerStateStore::load,
                PlayerStateStore::new,
                DATA_NAME
            );
        }

        @Contract("_ -> param1")
        @Override
        public @NotNull CompoundTag save(CompoundTag tag) {
            JsonObject root = new JsonObject();
            for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
                PlayerState state = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("gameType", state.gameType.getName());
                obj.addProperty("x", state.x);
                obj.addProperty("y", state.y);
                obj.addProperty("z", state.z);
                obj.addProperty("yRot", state.yRot);
                obj.addProperty("xRot", state.xRot);
                obj.addProperty("dimension", state.dimension.location().toString());
                root.add(entry.getKey().toString(), obj);
            }
            tag.putString("data", GSON.toJson(root));
            return tag;
        }

        public static PlayerStateStore load(@NotNull CompoundTag tag) {
            PlayerStateStore store = new PlayerStateStore();
            String json = tag.getString("data");
            if (json.isEmpty()) return store;

            try {
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root == null) return store;

                int count = 0;
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        JsonObject obj = entry.getValue().getAsJsonObject();

                        GameType gameType = GameType.byName(obj.get("gameType").getAsString());

                        double x = obj.get("x").getAsDouble();
                        double y = obj.get("y").getAsDouble();
                        double z = obj.get("z").getAsDouble();
                        float yRot = obj.get("yRot").getAsFloat();
                        float xRot = obj.get("xRot").getAsFloat();
                        ResourceKey<Level> dimension = ResourceKey.create(
                                Registries.DIMENSION,
                                new ResourceLocation(obj.get("dimension").getAsString())
                        );

                        store.states.put(uuid, new PlayerState(gameType, x, y, z, yRot, xRot, dimension));
                        count++;
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load player state for '{}', skipping", entry.getKey(), e);
                    }
                }
                LOGGER.info("Loaded {} saved player state(s)", count);
            } catch (Exception e) {
                LOGGER.error("Failed to parse saved player states", e);
            }

            return store;
        }
    }
}

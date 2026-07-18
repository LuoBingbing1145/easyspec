package lbb.easyspec;

import lbb.easyspec.config.Config;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasySpec implements ModInitializer {
    public static final String MOD_ID = "easyspec";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Config.load();
        LOGGER.info("EasySpec initialized!");
    }

    @Contract("_ -> new")
    public static @NotNull ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}

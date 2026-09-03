package ru.euphoria;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoFish implements ModInitializer {
    public static final AutoFish INSTANCE = new AutoFish();
    private static final Logger logger = LoggerFactory.getLogger("autofishplus");

    public AutoFish() {
    }

    @Override
    public void onInitialize() {
        logger.info("AutoFish+ initialized");
    }
}

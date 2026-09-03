package ru.euphoria.client;

import net.fabricmc.api.ClientModInitializer;
import ru.euphoria.config.ConfigManager;
import ru.euphoria.keyboard.KeyBindings;
import ru.euphoria.tools.AutoFish;
import ru.euphoria.tools.AutoFishHandler;

public final class AutoFishClient implements ClientModInitializer {
    public static final AutoFishClient INSTANCE = new AutoFishClient();

    public AutoFishClient() {
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.INSTANCE.loadConfig();
        KeyBindings.INSTANCE.init();
        AutoFish.INSTANCE.init();
        AutoFishHandler.INSTANCE.init();
        ru.euphoria.tools.AutoMacro.INSTANCE.init();
    }
}

package ru.euphoria.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenUtil {
    private ScreenUtil() {
    }

    public static void setScreen(Minecraft client, Screen screen) {
        if (client != null) {
            client.setScreenAndShow(screen);
        }
    }
}

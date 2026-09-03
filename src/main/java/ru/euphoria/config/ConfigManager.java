package ru.euphoria.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path configFile = FabricLoader.getInstance().getConfigDir().resolve("autofishplus.json");
    private static Config config = new Config();

    private ConfigManager() {
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        ConfigManager.config = config;
    }

    public void loadConfig() {
        if (Files.exists(configFile, new LinkOption[0])) {
            try (BufferedReader reader = Files.newBufferedReader(configFile)) {
                Config loaded = gson.fromJson((Reader) reader, Config.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (Exception e) {
                config = new Config();
            }
        } else {
            config = new Config();
        }
        saveConfig();
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
                gson.toJson(config, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static final class Config {
        private boolean enabled = true;
        private int recastDelay = 1000;
        private boolean dontNightFishing = false;
        private boolean replaceRod = true;
        private boolean antiBan = true;
        private boolean mouseJitter = true;

        public Config() {
        }

        public Config(boolean enabled, int recastDelay, boolean dontNightFishing, boolean replaceRod, boolean antiBan, boolean mouseJitter) {
            this.enabled = enabled;
            this.recastDelay = recastDelay;
            this.dontNightFishing = dontNightFishing;
            this.replaceRod = replaceRod;
            this.antiBan = antiBan;
            this.mouseJitter = mouseJitter;
        }

        public boolean getEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getRecastDelay() { return recastDelay; }
        public void setRecastDelay(int recastDelay) { this.recastDelay = recastDelay; }

        public boolean getDontNightFishing() { return dontNightFishing; }
        public void setDontNightFishing(boolean dontNightFishing) { this.dontNightFishing = dontNightFishing; }

        public boolean getReplaceRod() { return replaceRod; }
        public void setReplaceRod(boolean replaceRod) { this.replaceRod = replaceRod; }

        public boolean getAntiBan() { return antiBan; }
        public void setAntiBan(boolean antiBan) { this.antiBan = antiBan; }

        public boolean getMouseJitter() { return mouseJitter; }
        public void setMouseJitter(boolean mouseJitter) { this.mouseJitter = mouseJitter; }
    }
}

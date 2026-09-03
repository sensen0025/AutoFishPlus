package ru.euphoria.keyboard;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ru.euphoria.config.ConfigManager;
import ru.euphoria.config.ConfigScreen;
import ru.euphoria.tools.AutoMacro;

public final class KeyBindings {
    public static final KeyBindings INSTANCE = new KeyBindings();
    private static final KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autofishplus", "autofish"));
    private static final KeyMapping toggleMod = safeRegisterKey(new KeyMapping("keybinding.autofishplus.enabled", 74, category));
    private static final KeyMapping openConfigScreen = safeRegisterKey(new KeyMapping("keybinding.autofishplus.open_config", 39, category));

    private KeyBindings() {
    }

    private static KeyMapping safeRegisterKey(KeyMapping key) {
        try {
            Class<?> clazz = Class.forName("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper");
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals("registerKeyBinding") && m.getParameterCount() == 1 && Modifier.isStatic(m.getModifiers())) {
                    return (KeyMapping) m.invoke(null, key);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> clazz = Class.forName("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper");
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals("registerKeyMapping") && m.getParameterCount() == 1 && Modifier.isStatic(m.getModifiers())) {
                    return (KeyMapping) m.invoke(null, key);
                }
            }
        } catch (Throwable ignored) {
        }
        return key;
    }

    public void init() {
        // 1. 原生键盘/手机虚拟键盘映射的 J 键监听
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null) return;
            while (toggleMod.consumeClick()) {
                toggleMod(client);
            }
            while (openConfigScreen.consumeClick()) {
                openConfig(client);
            }
        });

        // 2. 客户端指令 /autofish 与 /af 注册（专为手机触屏与便捷切换适配）
        try {
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("autofish")
                    .executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    })
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle").executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("run").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.start();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("start").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.start();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("auto").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.start();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("stop").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.stop();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config").executes(ctx -> {
                        openConfig(Minecraft.getInstance());
                        return 1;
                    }))
                );

                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("af")
                    .executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    })
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle").executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("run").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.start();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("start").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.start();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("auto").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.start();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("stop").executes(ctx -> {
                        ru.euphoria.tools.AutoMacro.INSTANCE.stop();
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config").executes(ctx -> {
                        openConfig(Minecraft.getInstance());
                        return 1;
                    }))
                );
            });
        } catch (Throwable ignored) {
        }
    }

    public static void toggleMod(Minecraft client) {
        if (client == null) client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        LocalPlayer player = client.player;

        boolean newEnabled = !ConfigManager.INSTANCE.getConfig().getEnabled();
        ConfigManager.INSTANCE.getConfig().setEnabled(newEnabled);
        ConfigManager.INSTANCE.saveConfig();

        MutableComponent status = newEnabled ?
            Component.translatable("actionbar.autofishplus.on").withStyle(ChatFormatting.GREEN) :
            Component.translatable("actionbar.autofishplus.off").withStyle(ChatFormatting.RED);

        MutableComponent message = Component.literal("AutoFish+ ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY))
            .append(status);

        AutoMacro.sendOverlay(client, message);
    }

    public static void openConfig(Minecraft client) {
        if (client == null) client = Minecraft.getInstance();
        if (client == null) return;
        client.setScreenAndShow(ConfigScreen.Companion.create(null));
    }
}

package ru.euphoria.keyboard;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        // 1. 原生键盘/虚拟按键的 J 键监听
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null) return;
            while (toggleMod.consumeClick()) {
                toggleMod(client);
            }
            while (openConfigScreen.consumeClick()) {
                openConfig(client);
            }
        });

        // 2. 注册 Fabric 客户端指令 (/af 与 /autofish)
        registerClientCommands();

        // 3. 注册外发消息拦截器（双重保险，防止在 Hypixel 等服务器因命令树同步导致客户端命令被发至服务端报 Unknown command）
        registerMessageInterceptors();
    }

    private static void registerClientCommands() {
        try {
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                // /autofish 命令树
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("autofish")
                    .executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    })
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle").executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("on").executes(ctx -> {
                        setModState(Minecraft.getInstance(), true);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("off").executes(ctx -> {
                        setModState(Minecraft.getInstance(), false);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("start").executes(ctx -> {
                        setModState(Minecraft.getInstance(), true);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("stop").executes(ctx -> {
                        setModState(Minecraft.getInstance(), false);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("run").executes(ctx -> {
                        AutoMacro.sendOverlay(Minecraft.getInstance(), "§e[AutoFish+] §c/af run 宏寻路已屏蔽。所有功能（自动钓鱼 + 神话鱼QTE + Anti-AFK）已并入 J 键或 /af toggle！");
                        AutoMacro.sendMessage(Minecraft.getInstance(), "§e[AutoFish+] §c/af run 宏寻路已屏蔽。所有功能（自动钓鱼 + 神话鱼QTE + Anti-AFK）已并入 J 键或 /af toggle！");
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("auto").executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config").executes(ctx -> {
                        openConfig(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("settings").executes(ctx -> {
                        openConfig(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("help").executes(ctx -> {
                        showHelp(Minecraft.getInstance());
                        return 1;
                    }))
                );

                // /af 命令树
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("af")
                    .executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    })
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle").executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("on").executes(ctx -> {
                        setModState(Minecraft.getInstance(), true);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("off").executes(ctx -> {
                        setModState(Minecraft.getInstance(), false);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("start").executes(ctx -> {
                        setModState(Minecraft.getInstance(), true);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("stop").executes(ctx -> {
                        setModState(Minecraft.getInstance(), false);
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("run").executes(ctx -> {
                        AutoMacro.sendOverlay(Minecraft.getInstance(), "§e[AutoFish+] §c/af run 宏寻路已屏蔽。所有功能（自动钓鱼 + 神话鱼QTE + Anti-AFK）已并入 J 键或 /af toggle！");
                        AutoMacro.sendMessage(Minecraft.getInstance(), "§e[AutoFish+] §c/af run 宏寻路已屏蔽。所有功能（自动钓鱼 + 神话鱼QTE + Anti-AFK）已并入 J 键或 /af toggle！");
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("auto").executes(ctx -> {
                        toggleMod(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config").executes(ctx -> {
                        openConfig(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("settings").executes(ctx -> {
                        openConfig(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("help").executes(ctx -> {
                        showHelp(Minecraft.getInstance());
                        return 1;
                    }))
                );
            });
        } catch (Throwable t) {
            System.err.println("[AutoFish+] ClientCommandRegistrationCallback error: " + t.getMessage());
        }
    }

    private static void registerMessageInterceptors() {
        // 拦截客户端命令包
        try {
            net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
                if (handleCommandString(command)) {
                    return false; // 已被 AutoFish+ 捕获处理，拦截取消外发
                }
                return true;
            });
        } catch (Throwable t) {
            System.err.println("[AutoFish+] ALLOW_COMMAND interceptor error: " + t.getMessage());
        }

        // 拦截客户端聊天包（兼容部分客户端或直接在聊天栏输入的情形）
        try {
            net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
                if (message != null && (message.startsWith("/") || message.toLowerCase().startsWith("af") || message.toLowerCase().startsWith("autofish"))) {
                    String cmd = message.startsWith("/") ? message.substring(1) : message;
                    if (handleCommandString(cmd)) {
                        return false; // 已被 AutoFish+ 捕获处理，拦截取消外发
                    }
                }
                return true;
            });
        } catch (Throwable t) {
            System.err.println("[AutoFish+] ALLOW_CHAT interceptor error: " + t.getMessage());
        }
    }

    public static boolean handleCommandString(String raw) {
        if (raw == null) return false;
        String line = raw.trim();
        if (line.startsWith("/")) {
            line = line.substring(1).trim();
        }
        if (line.isEmpty()) return false;

        String[] parts = line.split("\\s+");
        String root = parts[0].toLowerCase();
        if (!root.equals("af") && !root.equals("autofish")) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (parts.length == 1) {
            toggleMod(client);
            return true;
        }

        String sub = parts[1].toLowerCase();
        switch (sub) {
            case "toggle":
                toggleMod(client);
                return true;
            case "on":
            case "start":
            case "enable":
                setModState(client, true);
                return true;
            case "off":
            case "stop":
            case "disable":
                setModState(client, false);
                return true;
            case "auto":
                toggleMod(client);
                return true;
            case "run":
                AutoMacro.sendOverlay(client, "§e[AutoFish+] §c/af run 宏寻路已屏蔽。所有功能（自动钓鱼 + 神话鱼QTE + Anti-AFK）已并入 J 键或 /af toggle！");
                AutoMacro.sendMessage(client, "§e[AutoFish+] §c/af run 宏寻路已屏蔽。所有功能（自动钓鱼 + 神话鱼QTE + Anti-AFK）已并入 J 键或 /af toggle！");
                return true;
            case "config":
            case "settings":
            case "gui":
            case "menu":
                openConfig(client);
                return true;
            case "help":
                showHelp(client);
                return true;
            default:
                toggleMod(client);
                return true;
        }
    }

    public static void showHelp(Minecraft client) {
        AutoMacro.sendMessage(client, "§6=== AutoFish+ 指令与按键指南 ===");
        AutoMacro.sendMessage(client, "§a/af toggle §7- 切换自动钓鱼开/关 (与键盘 J 键完全等效)");
        AutoMacro.sendMessage(client, "§a/af §7- 无参数快速切换开/关");
        AutoMacro.sendMessage(client, "§a/af on §7/ §c/af off §7- 显式开启或关闭");
        AutoMacro.sendMessage(client, "§a/af config §7- 打开详细功能配置界面");
        AutoMacro.sendMessage(client, "§7已集成：自动钓鱼、神话鱼QTE（绿点红停）、原地防挂机移动(Anti-AFK)");
    }

    public static void setModState(Minecraft client, boolean enable) {
        if (client == null) client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        LocalPlayer player = client.player;

        boolean current = ConfigManager.INSTANCE.getConfig().getEnabled();
        if (current == enable) {
            String stateStr = enable ? "§a开启" : "§c关闭";
            AutoMacro.sendOverlay(client, "§6AutoFish+ • 状态已处于: " + stateStr);
            AutoMacro.sendMessage(client, "§6[AutoFish+] §7当前状态已处于: " + stateStr);
            return;
        }

        ConfigManager.INSTANCE.getConfig().setEnabled(enable);
        ConfigManager.INSTANCE.saveConfig();

        if (enable) {
            AutoMacro.INSTANCE.startFishingMode(client, player);
            AutoMacro.sendOverlay(client, "§6AutoFish+ • §a已开启 (自动钓鱼 + 神话鱼QTE + Anti-AFK)");
            AutoMacro.sendMessage(client, "§6[AutoFish+] §a已开启！(自动钓鱼 + 神话鱼QTE + 原地防挂机 Anti-AFK)");
        } else {
            AutoMacro.INSTANCE.stop();
            AutoMacro.sendOverlay(client, "§6AutoFish+ • §c已关闭");
            AutoMacro.sendMessage(client, "§6[AutoFish+] §c已关闭！");
        }
    }

    public static void toggleMod(Minecraft client) {
        if (client == null) client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        boolean newEnabled = !ConfigManager.INSTANCE.getConfig().getEnabled();
        setModState(client, newEnabled);
    }

    public static void openConfig(Minecraft client) {
        if (client == null) client = Minecraft.getInstance();
        if (client == null) return;
        client.setScreenAndShow(ConfigScreen.Companion.create(null));
    }
}

package ru.euphoria.tools;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import ru.euphoria.config.ConfigManager;

public final class AutoFish {
    public static final AutoFish INSTANCE = new AutoFish();
    private static int recastTicks;
    private static boolean waitingRecast;
    private static int reelTicks;
    private static boolean waitingReel;
    private static int lastClickTick = -1;

    private AutoFish() {
    }

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client == null) return;
        if (MythicalFishHandler.INSTANCE.isFighting()) {
            waitingReel = false;
            waitingRecast = false;
            return;
        }
        if (!ConfigManager.INSTANCE.getConfig().getEnabled()) {
            waitingReel = false;
            waitingRecast = false;
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) return;

        if (!(player.getMainHandItem().getItem() instanceof FishingRodItem)) {
            waitingReel = false;
            waitingRecast = false;
            return;
        }

        // 阶段一：收杆反应延迟倒计时（模拟真实人类神经反应时间，Ex-Gaussian 偏态分布）
        if (waitingReel) {
            if (--reelTicks <= 0) {
                waitingReel = false;
                if (player.fishing != null) {
                    this.rightClick(client); // 执行收杆
                    boolean antiBan = ConfigManager.INSTANCE.getConfig().getAntiBan();
                    int baseDelay = ConfigManager.INSTANCE.getConfig().getRecastDelay();
                    recastTicks = HumanTiming.calculateRecastTicks(baseDelay, antiBan);
                    waitingRecast = true; // 进入阶段二：抛竿等待
                }
            }
            return;
        }

        // 阶段二：再次抛竿倒计时（带非正态右偏长尾抖动与走神机制）
        if (waitingRecast) {
            if (--recastTicks <= 0) {
                waitingRecast = false;
                this.rightClick(client); // 再次抛竿
            }
        }
    }

    public void onFishBite() {
        if (MythicalFishHandler.INSTANCE.isFighting()) {
            return;
        }
        if (!ConfigManager.INSTANCE.getConfig().getEnabled()) {
            return;
        }
        // 状态锁：已处于收杆反应中或抛竿倒计时中，直接忽略重复触发
        if (waitingReel || waitingRecast) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        LocalPlayer player = client.player;
        if (player == null || player.fishing == null) return;

        ClientLevel level = client.level;
        if (level == null) return;
        if (ConfigManager.INSTANCE.getConfig().getDontNightFishing() && level.isDarkOutside()) {
            return;
        }

        if (ConfigManager.INSTANCE.getConfig().getAntiBan()) {
            // 开启防封：计算非正态人类反应时间 (3~8 ticks, 165~500ms)
            reelTicks = HumanTiming.calculateReactionTicks();
            waitingReel = true;
        } else {
            // 关闭防封：瞬时收杆
            this.rightClick(client);
            recastTicks = ConfigManager.INSTANCE.getConfig().getRecastDelay() / 50 + 1;
            waitingRecast = true;
        }
    }

    private void rightClick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        int currentTick = player.tickCount;
        if (currentTick == lastClickTick) {
            return;
        }
        lastClickTick = currentTick;

        // 如果处于 AutoMacro 自动化宏模式，精准朝向正北方抛竿
        if (AutoMacro.INSTANCE.isActive()) {
            player.setYRot(180.0f);
            player.setXRot(-2.0f);
        } else if (ConfigManager.INSTANCE.getConfig().getAntiBan() && ConfigManager.INSTANCE.getConfig().getMouseJitter()) {
            HumanTiming.applyMouseMicroJitter(player);
        }

        gameMode.useItem((Player) player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);
    }
}

package ru.euphoria.tools;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import ru.euphoria.config.ConfigManager;

public final class MythicalFishHandler {
    public static final MythicalFishHandler INSTANCE = new MythicalFishHandler();

    // 战斗状态机
    private boolean fighting = false;
    private int fightingTicks = 0;
    private int ticksWithoutFish = 0;
    private int clickTimer = 0;
    private int postFightRecastTimer = 0;
    private long lastNoticeTime = 0;
    private String currentFishName = "";

    public enum FightPhase {
        NONE,
        GREEN_REEL, // 绿灯阶段：快速收线削减 HP
        RED_STOP    // 红灯阶段：绝对停手防止过热爆竿
    }

    private FightPhase currentPhase = FightPhase.NONE;

    private MythicalFishHandler() {
    }

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public boolean isFighting() {
        return this.fighting;
    }

    public FightPhase getCurrentPhase() {
        return this.currentPhase;
    }

    public void onTick(Minecraft client) {
        if (client == null) return;
        try {
            LocalPlayer player = client.player;
            if (player == null || client.level == null) {
                this.fighting = false;
                this.currentPhase = FightPhase.NONE;
                return;
            }

            // 处理战斗胜利/结束后的自动补抛一竿倒计时
            if (this.postFightRecastTimer > 0) {
                if (--this.postFightRecastTimer == 0) {
                    if (player.fishing == null && ConfigManager.INSTANCE.getConfig().getEnabled()) {
                        if (AutoMacro.INSTANCE.isActive()) {
                            player.setYRot(180.0f);
                            player.setXRot(-2.0f);
                        }
                        MultiPlayerGameMode gm = client.gameMode;
                        if (gm != null) {
                            gm.useItem(player, InteractionHand.MAIN_HAND);
                            player.swing(InteractionHand.MAIN_HAND);
                            sendOverlay(client, "§e[AutoFish+] §a神话鱼战斗完成！已自动重新抛竿，恢复挂机！");
                        }
                    }
                }
            }

            FishingHook bobber = player.fishing;
            if (bobber == null) {
                if (this.fighting) {
                    if (++this.ticksWithoutFish >= 20) {
                        endFight(client, "浮漂已收回");
                    }
                }
                return;
            }

            // 浮漂存在，获取浮漂坐标
            double bx = bobber.getX();
            double by = bobber.getY();
            double bz = bobber.getZ();

            boolean foundFishEntity = false;
            boolean detectedRed = false;
            boolean detectedGreen = false;
            String detectedName = "";

            Iterable<Entity> entities = client.level.entitiesForRendering();
            if (entities != null) {
                for (Entity entity : entities) {
                    if (entity == null || entity == player || entity == bobber) continue;

                    // 纯坐标三维欧氏距离平方判定：稳定可靠，全版本一致，无任何临时对象与错误风险
                    double dx = entity.getX() - bx;
                    double dy = entity.getY() - by;
                    double dz = entity.getZ() - bz;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > 42.25) continue; // 限制在 6.5 格范围内

                    String combined = getEntityCombinedText(entity);
                    if (combined.isEmpty()) continue;

                    // 匹配神话鱼或战斗血条特征
                    if (isMythicalFish(combined) || isFightIndicator(combined)) {
                        foundFishEntity = true;
                        if (detectedName.isEmpty() && isMythicalFish(combined)) {
                            detectedName = extractFishName(combined);
                        }

                        Component comp = getEntityComponent(entity);

                        // 阶段与颜色检测
                        if (checkIsRed(comp, combined)) {
                            detectedRed = true;
                        }
                        if (checkIsGreen(comp, combined)) {
                            detectedGreen = true;
                        }
                    }
                }
            }

            if (foundFishEntity) {
                this.ticksWithoutFish = 0;
                this.fightingTicks++;

                if (!this.fighting) {
                    this.fighting = true;
                    this.fightingTicks = 0;
                    this.currentFishName = detectedName.isEmpty() ? "神话鱼" : detectedName;
                    sendOverlay(client, "§6[AutoFish+] §d发现 " + this.currentFishName + "！接管钓鱼 QTE 拉扯状态机！");
                }

                // 状态仲裁：红灯最高优先级（绝对防爆竿原则，一旦有红色信号即停手）
                if (detectedRed) {
                    this.currentPhase = FightPhase.RED_STOP;
                } else if (detectedGreen) {
                    this.currentPhase = FightPhase.GREEN_REEL;
                } else {
                    // 未能完全确定时，默认保持停手（安全第一）
                    this.currentPhase = FightPhase.RED_STOP;
                }

                handleFightAction(client, player);

                // 超时保护（Hypixel 主大厅神话鱼战斗通常在 60 秒内结束，做 75 秒安全兜底）
                if (this.fightingTicks > 1500) {
                    endFight(client, "战斗超时保护");
                }
            } else {
                if (this.fighting) {
                    if (++this.ticksWithoutFish >= 25) {
                        endFight(client, "神话鱼已成功捕获");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private String getEntityCombinedText(Entity entity) {
        if (entity == null) return "";
        StringBuilder sb = new StringBuilder();
        try {
            for (Method m : entity.getClass().getMethods()) {
                String name = m.getName();
                if ((name.equals("method_5797") || name.equals("getCustomName") ||
                     name.equals("method_5476") || name.equals("getDisplayName") ||
                     name.equals("method_5477") || name.equals("getName")) && m.getParameterCount() == 0) {
                    Object res = m.invoke(entity);
                    if (res instanceof Component comp) {
                        sb.append(comp.getString()).append(" ");
                    } else if (res != null) {
                        sb.append(res.toString()).append(" ");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return sb.toString().trim();
    }

    private Component getEntityComponent(Entity entity) {
        if (entity == null) return null;
        try {
            for (Method m : entity.getClass().getMethods()) {
                String name = m.getName();
                if ((name.equals("method_5797") || name.equals("getCustomName") ||
                     name.equals("method_5476") || name.equals("getDisplayName")) && m.getParameterCount() == 0) {
                    Object res = m.invoke(entity);
                    if (res instanceof Component comp) {
                        return comp;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void handleFightAction(Minecraft client, LocalPlayer player) {
        long now = System.currentTimeMillis();
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        // 如果 AutoMacro 激活，锁定正北朝向
        if (AutoMacro.INSTANCE.isActive()) {
            player.setYRot(180.0f);
            player.setXRot(-2.0f);
        }

        if (this.currentPhase == FightPhase.RED_STOP) {
            // 红灯阶段：绝对停止任何右键输入，防止张力/过热爆竿
            this.clickTimer = 0;
            if (now - this.lastNoticeTime >= 1000) {
                this.lastNoticeTime = now;
                sendOverlay(client, "§c[神话鱼-" + this.currentFishName + "] 红色阶段 (STOP) - 立即停手！严防爆竿！");
            }
        } else if (this.currentPhase == FightPhase.GREEN_REEL) {
            // 绿灯阶段：以每 3 ticks 一次（~6.6 CPS）稳健快速削减神话鱼 HP
            this.clickTimer++;
            if (this.clickTimer >= 3) {
                this.clickTimer = 0;
                gameMode.useItem(player, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (now - this.lastNoticeTime >= 1000) {
                this.lastNoticeTime = now;
                sendOverlay(client, "§a[神话鱼-" + this.currentFishName + "] 绿色阶段 (REEL) - 快速点按收线中...");
            }
        }
    }

    private void endFight(Minecraft client, String reason) {
        this.fighting = false;
        this.fightingTicks = 0;
        this.ticksWithoutFish = 0;
        this.clickTimer = 0;
        this.currentPhase = FightPhase.NONE;
        sendOverlay(client, "§6[AutoFish+] §a神话鱼拉扯战斗结束 (" + reason + ")！1.2秒后自动补抛一竿恢复挂机！");
        this.postFightRecastTimer = 25; // 1.25秒后自动重新抛竿
    }

    private boolean isMythicalFish(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        return lower.contains("archimedes") ||
               lower.contains("demeter") ||
               lower.contains("helios") ||
               lower.contains("hades") ||
               lower.contains("nyx") ||
               lower.contains("selene") ||
               lower.contains("zeus") ||
               lower.contains("aphrodite") ||
               lower.contains("daedalus") ||
               lower.contains("mythic") ||
               lower.contains("阿基米德") ||
               lower.contains("德墨忒尔") ||
               lower.contains("赫利俄斯") ||
               lower.contains("哈迪斯") ||
               lower.contains("倪克斯") ||
               lower.contains("塞勒涅") ||
               lower.contains("宙斯") ||
               lower.contains("阿佛洛狄忒") ||
               lower.contains("神话鱼");
    }

    private String extractFishName(String text) {
        if (text == null) return "神话鱼";
        String lower = text.toLowerCase();
        if (lower.contains("archimedes") || lower.contains("daedalus") || lower.contains("阿基米德")) return "Archimedes";
        if (lower.contains("demeter") || lower.contains("德墨忒尔")) return "Demeter";
        if (lower.contains("helios") || lower.contains("赫利俄斯")) return "Helios";
        if (lower.contains("hades") || lower.contains("哈迪斯")) return "Hades";
        if (lower.contains("nyx") || lower.contains("倪克斯")) return "Nyx";
        if (lower.contains("selene") || lower.contains("塞勒涅")) return "Selene";
        if (lower.contains("zeus") || lower.contains("宙斯")) return "Zeus";
        if (lower.contains("aphrodite") || lower.contains("阿佛洛狄忒")) return "Aphrodite";
        return "MythicalFish";
    }

    private boolean isFightIndicator(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        return lower.contains("reel") ||
               lower.contains("stop") ||
               lower.contains("pull") ||
               lower.contains("hold") ||
               lower.contains("tension") ||
               lower.contains("heat") ||
               lower.contains("|||") ||
               lower.contains("■■■") ||
               lower.contains("❤❤") ||
               lower.contains("hp:") ||
               lower.contains("hp ");
    }

    private boolean checkIsRed(Component comp, String text) {
        if (text != null) {
            String lower = text.toLowerCase();
            if (lower.contains("§c") || lower.contains("§4")) {
                if (lower.contains("stop") || lower.contains("hold") || lower.contains("release") || 
                    lower.contains("|||") || lower.contains("■") || lower.contains("❤") || lower.contains("hp")) {
                    return true;
                }
            }
            if (lower.contains("stop") || lower.contains("hold") || lower.contains("release") ||
                lower.contains("停") || lower.contains("放") || lower.contains("断")) {
                return true;
            }
        }
        if (comp != null) {
            if (hasColor(comp, 180, 100, true)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIsGreen(Component comp, String text) {
        if (text != null) {
            String lower = text.toLowerCase();
            if (lower.contains("§a") || lower.contains("§2")) {
                if (lower.contains("reel") || lower.contains("pull") || lower.contains("click") ||
                    lower.contains("|||") || lower.contains("■") || lower.contains("❤") || lower.contains("hp")) {
                    return true;
                }
            }
            if (lower.contains("reel") || lower.contains("pull") || lower.contains("click") ||
                lower.contains("拉") || lower.contains("收")) {
                return true;
            }
        }
        if (comp != null) {
            if (hasColor(comp, 100, 180, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasColor(Component comp, int rThreshold, int gThreshold, boolean expectRed) {
        if (comp == null) return false;
        Style style = comp.getStyle();
        if (style != null) {
            TextColor textColor = style.getColor();
            if (textColor != null) {
                int rgb = textColor.getValue();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (expectRed && r >= rThreshold && g <= gThreshold) {
                    return true;
                }
                if (!expectRed && g >= gThreshold && r <= rThreshold) {
                    return true;
                }
            }
        }
        List<Component> siblings = comp.getSiblings();
        if (siblings != null && !siblings.isEmpty()) {
            for (Component sib : siblings) {
                if (hasColor(sib, rThreshold, gThreshold, expectRed)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendOverlay(Minecraft client, String message) {
        AutoMacro.sendOverlay(client, message);
    }
}

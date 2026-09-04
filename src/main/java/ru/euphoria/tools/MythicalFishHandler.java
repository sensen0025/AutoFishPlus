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
        GREEN_REEL, // 绿灯阶段：进度条为绿色，快速连点削减 HP
        RED_STOP    // 红灯阶段：进度条为红色，绝对停手防止过热爆竿/逃跑
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
                            player.setXRot(28.0f); // 调低 30 度
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
            int detectedPhase = 0; // 0: None/Unknown, 1: Green, 2: Red
            String detectedName = "";
            String detectedBarText = "";

            Iterable<Entity> entities = client.level.entitiesForRendering();
            if (entities != null) {
                for (Entity entity : entities) {
                    if (entity == null || entity == player || entity == bobber) continue;

                    // 纯坐标距离判定：限制在浮漂周围 8.0 格范围内 (8*8 = 64.0)
                    double dx = entity.getX() - bx;
                    double dy = entity.getY() - by;
                    double dz = entity.getZ() - bz;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > 64.0) continue; 

                    String combined = getEntityCombinedText(entity);
                    if (combined.isEmpty()) continue;

                    // 核心匹配：检测进度条特征或神话鱼名称
                    if (isProgressBar(combined) || isMythicalFish(combined)) {
                        foundFishEntity = true;
                        detectedBarText = combined;
                        if (detectedName.isEmpty() && isMythicalFish(combined)) {
                            detectedName = extractFishName(combined);
                        }

                        Component comp = getEntityComponent(entity);
                        int p = analyzeProgressColor(comp, combined);
                        if (p == 2) {
                            detectedPhase = 2; // 红灯最高优先级
                        } else if (p == 1 && detectedPhase != 2) {
                            detectedPhase = 1;
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
                    System.out.println("[MythicalFish] 捕获到神话鱼拉扯进度条: [" + detectedBarText + "]");
                    sendOverlay(client, "§6[AutoFish+] §d发现 " + this.currentFishName + "！接管钓鱼 QTE 拉扯状态机！");
                }

                // 状态仲裁：红灯最高优先级（绝对防爆竿/防脱线原则）
                if (detectedPhase == 2) {
                    this.currentPhase = FightPhase.RED_STOP;
                } else if (detectedPhase == 1) {
                    this.currentPhase = FightPhase.GREEN_REEL;
                } else {
                    // 未能完全确定时，默认保持停手（安全第一）
                    this.currentPhase = FightPhase.RED_STOP;
                }

                handleFightAction(client, player, detectedBarText);

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

    private boolean isProgressBar(String text) {
        if (text == null || text.isEmpty()) return false;
        String trimmed = text.trim();
        // 包含经典进度条符号
        if (trimmed.contains("|") || trimmed.contains("■") || trimmed.contains("█") ||
            trimmed.contains("❤") || trimmed.contains("%") || trimmed.contains("===") ||
            trimmed.contains("---") || trimmed.contains("///")) {
            return true;
        }
        String lower = trimmed.toLowerCase();
        if (lower.contains("reel") || lower.contains("stop") || lower.contains("pull") ||
            lower.contains("hold") || lower.contains("hp:") || lower.contains("hp ")) {
            return true;
        }
        return false;
    }

    private int analyzeProgressColor(Component comp, String text) {
        // 返回值: 1 = GREEN, 2 = RED, 0 = UNKNOWN
        int redScore = 0;
        int greenScore = 0;

        if (text != null) {
            String lower = text.toLowerCase();
            // 关键词直接判定
            if (lower.contains("stop") || lower.contains("hold") || lower.contains("release") ||
                lower.contains("停") || lower.contains("放") || lower.contains("断")) {
                return 2;
            }
            if (lower.contains("reel") || lower.contains("pull") || lower.contains("click") ||
                lower.contains("拉") || lower.contains("收")) {
                return 1;
            }

            // 分析颜色代码
            int len = text.length();
            for (int i = 0; i < len - 1; i++) {
                if (text.charAt(i) == '§') {
                    char c = Character.toLowerCase(text.charAt(i + 1));
                    if (c == 'c' || c == '4') { // 红色
                        redScore += 3;
                    } else if (c == 'a' || c == '2') { // 绿色
                        greenScore += 3;
                    }
                }
            }
        }

        // 分析 Component Style
        if (comp != null) {
            int[] compScores = countComponentColors(comp);
            greenScore += compScores[0];
            redScore += compScores[1];
        }

        if (redScore > 0 && redScore >= greenScore) {
            return 2; // 红色
        }
        if (greenScore > 0 && greenScore > redScore) {
            return 1; // 绿色
        }
        return 0;
    }

    private int[] countComponentColors(Component comp) {
        int green = 0;
        int red = 0;
        if (comp == null) return new int[]{0, 0};

        Style style = comp.getStyle();
        if (style != null) {
            TextColor textColor = style.getColor();
            if (textColor != null) {
                int rgb = textColor.getValue();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                if (r >= 160 && g <= 110) {
                    red += 2;
                } else if (g >= 160 && r <= 110) {
                    green += 2;
                }
            }
        }

        List<Component> siblings = comp.getSiblings();
        if (siblings != null && !siblings.isEmpty()) {
            for (Component sib : siblings) {
                int[] sub = countComponentColors(sib);
                green += sub[0];
                red += sub[1];
            }
        }
        return new int[]{green, red};
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

    private void handleFightAction(Minecraft client, LocalPlayer player, String barText) {
        long now = System.currentTimeMillis();
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        // 如果 AutoMacro 激活，锁定正北且调低 30 度的朝向 (Pitch 28.0)
        if (AutoMacro.INSTANCE.isActive()) {
            player.setYRot(180.0f);
            player.setXRot(28.0f);
        }

        if (this.currentPhase == FightPhase.RED_STOP) {
            // 红灯阶段：绝对停止任何右键输入，防止张力/过热爆竿
            this.clickTimer = 0;
            if (now - this.lastNoticeTime >= 1000) {
                this.lastNoticeTime = now;
                System.out.println("[MythicalFish] 进度条红色 -> 立即停手！" + barText);
                sendOverlay(client, "§c[神话鱼-" + this.currentFishName + "] 红色进度条 (STOP) - 立即停手！严防爆竿！");
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
                System.out.println("[MythicalFish] 进度条绿色 -> 连点收线中！" + barText);
                sendOverlay(client, "§a[神话鱼-" + this.currentFishName + "] 绿色进度条 (REEL) - 快速连点收线中...");
            }
        }
    }

    private void endFight(Minecraft client, String reason) {
        this.fighting = false;
        this.fightingTicks = 0;
        this.ticksWithoutFish = 0;
        this.clickTimer = 0;
        this.currentPhase = FightPhase.NONE;
        System.out.println("[MythicalFish] 战斗结束: " + reason);
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

    private void sendOverlay(Minecraft client, String message) {
        AutoMacro.sendOverlay(client, message);
    }
}

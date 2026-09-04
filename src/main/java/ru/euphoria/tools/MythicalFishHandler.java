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
    private long lastDebugProbeTime = 0;
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
        System.out.println("[MythicalFish] Handler registered to END_CLIENT_TICK successfully!");
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
                            player.setXRot(28.0f); // 俯角 28 度
                        }
                        MultiPlayerGameMode gm = client.gameMode;
                        if (gm != null) {
                            gm.useItem(player, InteractionHand.MAIN_HAND);
                            player.swing(InteractionHand.MAIN_HAND);
                            sendOverlay(client, "§e[AutoFish+] §b神话鱼战斗完成！已自动重新抛竿，恢复挂机！");
                        }
                    }
                }
            }

            // 注意：使用 Entity 接收浮漂对象，确保 getX/Y/Z 准确映射至 Entity.method_23317 等 Intermediary 方法，防止 NoSuchMethodError 崩溃
            Entity bobber = player.fishing;
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
            double closestDist = 999.0;

            long now = System.currentTimeMillis();
            boolean shouldLogProbe = (now - this.lastDebugProbeTime >= 2000);

            Iterable<Entity> entities = client.level.entitiesForRendering();
            if (entities != null) {
                for (Entity entity : entities) {
                    if (entity == null || entity == player || entity == bobber) continue;

                    double dx = entity.getX() - bx;
                    double dy = entity.getY() - by;
                    double dz = entity.getZ() - bz;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    double hDistSq = dx * dx + dz * dz;

                    // 检测范围扩大至 11 格 (11*11 = 121.0)
                    // 兼容 3D 球体 11 格与水平圆柱 11 格（高低差允许 12 格）
                    boolean inRange = (distSq <= 121.0) || (hDistSq <= 121.0 && Math.abs(dy) <= 12.0);
                    if (!inRange) continue;

                    String combined = getEntityCombinedText(entity);
                    if (combined.isEmpty()) continue;

                    double currentDist = Math.sqrt(distSq);

                    // 核心匹配：检测进度条特征或神话鱼名称
                    if (isProgressBar(combined) || isMythicalFish(combined)) {
                        foundFishEntity = true;
                        if (currentDist < closestDist) {
                            closestDist = currentDist;
                            detectedBarText = combined;
                            if (isMythicalFish(combined)) {
                                detectedName = extractFishName(combined);
                            }
                        }

                        Component comp = getEntityComponent(entity);
                        int p = analyzeProgressColor(comp, combined);
                        if (p == 2) {
                            detectedPhase = 2; // 红灯最高优先级
                        } else if (p == 1 && detectedPhase != 2) {
                            detectedPhase = 1;
                        }
                    } else if (shouldLogProbe && !this.fighting) {
                        // 周期性调试探针：打印 11 格范围内尚未匹配到的带文本实体
                        System.out.println("[MythicalFish-Probe] 浮漂周围11格内发现实体: [" + combined + "] 距离: " + String.format("%.2f", currentDist));
                    }
                }
            }

            if (shouldLogProbe) {
                this.lastDebugProbeTime = now;
            }

            if (foundFishEntity) {
                this.ticksWithoutFish = 0;
                this.fightingTicks++;

                if (!this.fighting) {
                    this.fighting = true;
                    this.fightingTicks = 0;
                    this.currentFishName = detectedName.isEmpty() ? "神话鱼" : detectedName;
                    System.out.println("[MythicalFish] 捕获到神话鱼拉扯进度条: [" + detectedBarText + "] 距离: " + String.format("%.2f", closestDist));
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
        } catch (Throwable t) {
            System.err.println("[MythicalFish Error] " + t.getMessage());
            t.printStackTrace();
        }
    }

    private boolean isProgressBar(String text) {
        if (text == null || text.isEmpty()) return false;
        String trimmed = text.trim();
        // 包含经典进度条符号（ASCII、全角及 Unicode 块字符）
        if (trimmed.contains("|") || trimmed.contains("｜") || trimmed.contains("丨") ||
            trimmed.contains("■") || trimmed.contains("█") || trimmed.contains("▌") ||
            trimmed.contains("▍") || trimmed.contains("▎") || trimmed.contains("▏") ||
            trimmed.contains("▒") || trimmed.contains("▓") || trimmed.contains("░") ||
            trimmed.contains("❤") || trimmed.contains("%") || trimmed.contains("===") ||
            trimmed.contains("---") || trimmed.contains("///") ||
            trimmed.contains("IIIII") || trimmed.contains("lllll")) {
            return true;
        }
        String lower = trimmed.toLowerCase();
        if (lower.contains("reel") || lower.contains("stop") || lower.contains("pull") ||
            lower.contains("hold") || lower.contains("hp:") || lower.contains("hp ") ||
            lower.contains("收线") || lower.contains("停手") || lower.contains("拉扯")) {
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
            // 关键词直接判定（英文 + 中文）
            if (lower.contains("stop") || lower.contains("hold") || lower.contains("release") ||
                lower.contains("停") || lower.contains("放") || lower.contains("断")) {
                redScore += 10;
            }
            if (lower.contains("reel") || lower.contains("pull") || lower.contains("click") ||
                lower.contains("拉") || lower.contains("收")) {
                greenScore += 10;
            }

            // 分析颜色代码和 style 属性
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
            if (lower.contains("color=red") || lower.contains("color=dark_red") || lower.contains("#ff5555") || lower.contains("#aa0000")) {
                redScore += 5;
            }
            if (lower.contains("color=green") || lower.contains("color=dark_green") || lower.contains("#55ff55") || lower.contains("#00aa00")) {
                greenScore += 5;
            }
        }

        // 分析 Component Style 树
        if (comp != null) {
            int[] compScores = countComponentColors(comp);
            greenScore += compScores[0];
            redScore += compScores[1];
        }

        if (redScore > 0 && redScore >= greenScore) {
            return 2; // 红色（最高优先级停手）
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
                if (r >= 150 && g <= 120) {
                    red += 3;
                } else if (g >= 150 && r <= 120) {
                    green += 3;
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
                if (m.getParameterCount() == 0 && Component.class.isAssignableFrom(m.getReturnType())) {
                    Object res = m.invoke(entity);
                    if (res instanceof Component comp) {
                        sb.append(comp.getString()).append(" ");
                        sb.append(comp.toString()).append(" ");
                    }
                } else if (m.getParameterCount() == 0 && String.class.isAssignableFrom(m.getReturnType())) {
                    String name = m.getName();
                    if (name.contains("Name") || name.contains("Text") || name.contains("String")) {
                        Object res = m.invoke(entity);
                        if (res != null) {
                            sb.append(res.toString()).append(" ");
                        }
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
                if (m.getParameterCount() == 0 && Component.class.isAssignableFrom(m.getReturnType())) {
                    Object res = m.invoke(entity);
                    if (res instanceof Component comp) {
                        String s = comp.getString();
                        if (isProgressBar(s) || isMythicalFish(s)) {
                            return comp;
                        }
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

        // 如果 AutoMacro 激活，锁定正北且俯视水面的朝向 (Pitch 28.0)
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
                sendOverlay(client, "§b[神话鱼-" + this.currentFishName + "] 允许拉线 (REEL) - 快速连点收线中...");
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
        sendOverlay(client, "§6[AutoFish+] §b神话鱼拉扯战斗结束 (" + reason + ")！1.2秒后自动补抛一竿恢复挂机！");
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
               lower.contains("代达罗斯") ||
               lower.contains("日神") ||
               lower.contains("余烬") ||
               lower.contains("泰坦") ||
               lower.contains("神话");
    }

    private String extractFishName(String text) {
        if (text == null) return "神话鱼";
        String lower = text.toLowerCase();
        if (lower.contains("archimedes") || lower.contains("daedalus") || lower.contains("阿基米德")) return "Archimedes";
        if (lower.contains("demeter") || lower.contains("德墨忒尔")) return "Demeter";
        if (lower.contains("helios") || lower.contains("赫利俄斯") || lower.contains("日神") || lower.contains("余烬")) return "Helios";
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

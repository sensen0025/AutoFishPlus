package ru.euphoria.tools;

import java.util.Random;
import net.minecraft.client.player.LocalPlayer;

public final class HumanTiming {
    private static final Random random = new Random();

    /**
     * 人类真实反应时间模型 (Ex-Gaussian 分布模型)
     * 听觉/视觉刺激到手指按下微动的神经传导反应时间 (毫秒)
     * 特征：显著右偏态 (Skewness > 1.0)，峰值在 210~260ms，长尾可达 450~550ms
     */
    public static long getHumanReactionDelayMs() {
        // 1. 高斯部分 (Box-Muller 变换)
        double u1 = Math.max(1e-7, random.nextDouble());
        double u2 = random.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        double gaussian = 185.0 + 28.0 * z;

        // 2. 认知注意力涨落指数长尾部分 (逆变换采样)
        double u3 = Math.max(1e-7, random.nextDouble());
        double exponential = -75.0 * Math.log(u3);

        double total = gaussian + exponential;
        // 生理极限硬截断：人类视觉/听觉最快反应一般不低于 165ms，上限设为 580ms 防止脱钩
        if (total < 165.0) total = 165.0 + random.nextDouble() * 25.0;
        if (total > 580.0) total = 500.0 + random.nextDouble() * 70.0;
        return (long) total;
    }

    /**
     * 将反应时间转换为 Minecraft 客户端 Tick 数 (1 tick = 50ms)
     */
    public static int calculateReactionTicks() {
        long ms = getHumanReactionDelayMs();
        int ticks = (int) Math.round((double) ms / 50.0);
        return Math.max(3, ticks); // 至少 3 ticks (150ms)，一般在 4~8 ticks
    }

    /**
     * 计算非正态抛竿等待延迟 (Ticks)
     */
    public static int calculateRecastTicks(int baseDelayMs, boolean antiBan) {
        if (!antiBan) {
            return Math.max(1, baseDelayMs / 50 + 1);
        }
        // 非正态偏态长尾抖动
        double u = Math.max(1e-7, random.nextDouble());
        double jitter = -40.0 + 130.0 * Math.pow(-Math.log(u), 1.35);
        // 约 2.5% 的概率发生轻微走神/分心，增加 1000~2500ms 停顿
        if (random.nextDouble() < 0.025) {
            jitter += 1100.0 + random.nextDouble() * 1400.0;
        }
        double totalMs = (double) baseDelayMs + jitter;
        if (totalMs < 650.0) totalMs = 650.0 + random.nextDouble() * 100.0;
        int ticks = (int) Math.round(totalMs / 50.0);
        return Math.max(13, ticks);
    }

    /**
     * 模拟物理手部下压鼠标按键时产生的极微小视角偏移 (Micro Drift)
     * 消除机器人 bit-exact 0 度静态角度特征
     */
    public static void applyMouseMicroJitter(LocalPlayer player) {
        if (player == null) return;
        float yawOffset = (float) ((random.nextDouble() - 0.5) * 0.16);
        float pitchOffset = (float) ((random.nextDouble() - 0.5) * 0.10);
        player.setYRot(player.getYRot() + yawOffset);
        float newPitch = Math.max(-89.5f, Math.min(89.5f, player.getXRot() + pitchOffset));
        player.setXRot(newPitch);
    }
}

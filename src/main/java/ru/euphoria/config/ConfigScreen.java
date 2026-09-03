package ru.euphoria.config;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreen extends Screen {
    public static final Companion Companion = new Companion();
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("config.autofishplus.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = Math.max(16, this.height / 8);

        this.addRenderableOnly(new StringWidget(centerX - 150, y, 300, 20, this.title, this.font));

        // 1. 自动钓鱼总开关
        CycleButton<Boolean> enabledBtn = this.booleanButton(centerX - 100, y += 26, Component.translatable("config.autofishplus.enabled"), ConfigManager.INSTANCE.getConfig().getEnabled(), it -> ConfigManager.INSTANCE.getConfig().setEnabled(it));
        enabledBtn.setTooltip(Tooltip.create(Component.translatable("config.autofishplus.enabled.tooltip")));
        this.addRenderableWidget(enabledBtn);

        // 2. 抛竿基准延迟滑条
        this.addRenderableWidget(new RecastDelaySlider(centerX - 100, y += 24, 200, 20));

        // 3. 防封人手拟真 (非正态长尾延迟)
        CycleButton<Boolean> antiBanBtn = this.booleanButton(centerX - 100, y += 24, Component.translatable("config.autofishplus.antiban"), ConfigManager.INSTANCE.getConfig().getAntiBan(), it -> ConfigManager.INSTANCE.getConfig().setAntiBan(it));
        antiBanBtn.setTooltip(Tooltip.create(Component.translatable("config.autofishplus.antiban.tooltip")));
        this.addRenderableWidget(antiBanBtn);

        // 4. 模拟点击微抖动
        CycleButton<Boolean> jitterBtn = this.booleanButton(centerX - 100, y += 24, Component.translatable("config.autofishplus.mouse_jitter"), ConfigManager.INSTANCE.getConfig().getMouseJitter(), it -> ConfigManager.INSTANCE.getConfig().setMouseJitter(it));
        jitterBtn.setTooltip(Tooltip.create(Component.translatable("config.autofishplus.mouse_jitter.tooltip")));
        this.addRenderableWidget(jitterBtn);

        // 5. 夜间暂停
        CycleButton<Boolean> nightBtn = this.booleanButton(centerX - 100, y += 24, Component.translatable("config.autofishplus.dont_night_fishing"), ConfigManager.INSTANCE.getConfig().getDontNightFishing(), it -> ConfigManager.INSTANCE.getConfig().setDontNightFishing(it));
        nightBtn.setTooltip(Tooltip.create(Component.translatable("config.autofishplus.dont_night_fishing.tooltip")));
        this.addRenderableWidget(nightBtn);

        // 6. 坏杆自动替换
        CycleButton<Boolean> rodBtn = this.booleanButton(centerX - 100, y += 24, Component.translatable("config.autofishplus.replace_rod"), ConfigManager.INSTANCE.getConfig().getReplaceRod(), it -> ConfigManager.INSTANCE.getConfig().setReplaceRod(it));
        rodBtn.setTooltip(Tooltip.create(Component.translatable("config.autofishplus.replace_rod.tooltip")));
        this.addRenderableWidget(rodBtn);

        // 7. 保存并关闭按钮
        this.addRenderableWidget(Button.builder(Component.translatable("config.autofishplus.done"), it -> this.saveAndClose()).bounds(centerX - 100, y += 28, 200, 20).build());
    }

    @Override
    public void onClose() {
        this.saveAndClose();
    }

    private void saveAndClose() {
        ConfigManager.INSTANCE.saveConfig();
        ScreenUtil.setScreen(this.minecraft, this.parent);
    }

    private CycleButton<Boolean> booleanButton(int x, int y, Component label, boolean initialValue, Consumer<Boolean> save) {
        return CycleButton.onOffBuilder(initialValue).create(x, y, 200, 20, label, (btn, val) -> save.accept(val));
    }

    public static final class Companion {
        public Screen create(Screen parent) {
            return new ConfigScreen(parent);
        }
    }

    private static final class RecastDelaySlider extends AbstractSliderButton {
        public RecastDelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), ((double)ConfigManager.INSTANCE.getConfig().getRecastDelay() / 100.0 - 10.0) / 90.0);
            this.setTooltip(Tooltip.create(Component.translatable("config.autofishplus.recast_delay.tooltip")));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            float seconds = (float)this.getDelaySteps() / 10.0f;
            this.setMessage(Component.translatable("config.autofishplus.recast_delay").append(": ").append(Component.literal(seconds + " ")).append(Component.translatable("config.autofishplus.recast_delay_seconds")));
        }

        @Override
        protected void applyValue() {
            ConfigManager.INSTANCE.getConfig().setRecastDelay(this.getDelaySteps() * 100);
        }

        private int getDelaySteps() {
            int step = (int)(this.value * 90.0);
            if (step < 0) step = 0;
            if (step > 90) step = 90;
            return 10 + step;
        }
    }
}

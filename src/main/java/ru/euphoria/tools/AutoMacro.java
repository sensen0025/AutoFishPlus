package ru.euphoria.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.euphoria.config.ConfigManager;

public final class AutoMacro {
    public static final AutoMacro INSTANCE = new AutoMacro();

    public enum MacroState {
        IDLE,
        CHECK_ROD,
        NAVIGATING_TO_NPC,
        INTERACTING_WITH_NPC,
        BUYING_ROD,
        NAVIGATING_TO_FISHING,
        FISHING,
        AFK_STEP_BACK,
        AFK_STEP_FORWARD
    }

    private static final double NPC_X = 22.5;
    private static final double NPC_Y = 62.0;
    private static final double NPC_Z = -36.5;

    private static final double FISH_X = 36.5;
    private static final double FISH_Y = 59.0;
    private static final double FISH_Z = -60.5;

    private MacroState state = MacroState.IDLE;
    private int timer = 0;
    private int afkTimer = 0;
    private int noBobberTimer = 0;
    private double baseFishX = 36.5;
    private double baseFishZ = -60.0;
    private int rightClickCooldown = 0;

    private AutoMacro() {
    }

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public boolean isActive() {
        return this.state != MacroState.IDLE;
    }

    public MacroState getState() {
        return this.state;
    }

    public void start() {
        Minecraft client = Minecraft.getInstance();
        this.state = MacroState.CHECK_ROD;
        this.timer = 0;
        this.afkTimer = 0;
        this.noBobberTimer = 0;
        sendOverlay(client, "§e[AutoFish+] §a自动化宏已启动！正在检查鱼竿...");
    }

    public void stop() {
        Minecraft client = Minecraft.getInstance();
        this.state = MacroState.IDLE;
        this.timer = 0;
        this.afkTimer = 0;
        this.noBobberTimer = 0;
        setKeyUp(client, false);
        setKeyDown(client, false);
        cancelBaritone(client);
        ConfigManager.INSTANCE.getConfig().setEnabled(false);
        sendOverlay(client, "§e[AutoFish+] §c自动化宏已停止！");
    }

    public void toggle() {
        if (isActive()) {
            stop();
        } else {
            start();
        }
    }

    private void onTick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        LocalPlayer player = client.player;

        if (this.rightClickCooldown > 0) {
            this.rightClickCooldown--;
            if (this.rightClickCooldown == 0 && client.options != null && client.options.keyUse != null) {
                client.options.keyUse.setDown(false);
            }
        }

        if (this.state == MacroState.IDLE) {
            return;
        }

        this.timer++;

        switch (this.state) {
            case CHECK_ROD:
                handleCheckRod(client, player);
                break;
            case NAVIGATING_TO_NPC:
                handleNavigatingToNpc(client, player);
                break;
            case INTERACTING_WITH_NPC:
                handleInteractingWithNpc(client, player);
                break;
            case BUYING_ROD:
                handleBuyingRod(client, player);
                break;
            case NAVIGATING_TO_FISHING:
                handleNavigatingToFishing(client, player);
                break;
            case FISHING:
                handleFishingState(client, player);
                break;
            case AFK_STEP_BACK:
                handleAfkStepBack(client, player);
                break;
            case AFK_STEP_FORWARD:
                handleAfkStepForward(client, player);
                break;
        }
    }

    private void handleCheckRod(Minecraft client, LocalPlayer player) {
        if (hasFishingRod(player)) {
            equipFishingRod(client);
            sendBaritoneGoto(client, 36, 59, -60);
            sendOverlay(client, "§e[AutoFish+] §a检测到已有鱼竿，前往钓鱼点 (36, 59, -60)...");
            this.state = MacroState.NAVIGATING_TO_FISHING;
            this.timer = 0;
        } else {
            sendBaritoneGoto(client, 22, 62, -36);
            sendOverlay(client, "§e[AutoFish+] §c未检测到鱼竿，前往 NPC (22, 62, -36) 购买...");
            this.state = MacroState.NAVIGATING_TO_NPC;
            this.timer = 0;
        }
    }

    private void handleNavigatingToNpc(Minecraft client, LocalPlayer player) {
        double dx = player.getX() - NPC_X;
        double dz = player.getZ() - NPC_Z;
        double distSq = dx * dx + dz * dz;

        // 到达判定：水平距离 <= 1.4格，且Y轴误差 <= 2.0
        if (distSq <= 1.96 && Math.abs(player.getY() - NPC_Y) <= 2.0) {
            cancelBaritone(client);
            // 自动校准朝向 NPC（优先根据附近 NPC 实体坐标动态锁定，兜底朝向东偏正对 23, 62, -36）
            alignToNpc(client, player);
            sendOverlay(client, "§e[AutoFish+] §a已到达 NPC 处，精确对准 NPC 交互打开商店...");
            this.state = MacroState.INTERACTING_WITH_NPC;
            this.timer = 0;
            return;
        }

        // 防卡死超时（45秒重发一次寻路指令）
        if (this.timer > 900) {
            sendBaritoneGoto(client, 22, 62, -36);
            this.timer = 0;
        }
    }

    private void handleInteractingWithNpc(Minecraft client, LocalPlayer player) {
        alignToNpc(client, player);

        // 已经成功打开容器界面
        if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
            sendOverlay(client, "§e[AutoFish+] §aNPC 商店已打开，正在寻找并点击购买鱼竿...");
            this.state = MacroState.BUYING_ROD;
            this.timer = 0;
            return;
        }

        // 自动对准与交互
        if (this.timer == 8 || this.timer == 25 || this.timer == 45) {
            interactWithNpc(client, player);
        }

        // 扫射备选角度（如果48 ticks仍未打开，尝试西面 Yaw 90；62 ticks尝试东面 Yaw -90）
        if (this.timer == 50) {
            player.setYRot(90.0f);
            player.setXRot(5.0f);
            simulateRightClick(client, player);
        }
        if (this.timer == 65) {
            player.setYRot(-90.0f);
            player.setXRot(5.0f);
            simulateRightClick(client, player);
        }

        // 超时重试（如果80 ticks仍未打开）
        if (this.timer > 80) {
            sendOverlay(client, "§e[AutoFish+] §e重新尝试与 NPC 交互...");
            this.timer = 0;
        }
    }

    private void handleBuyingRod(Minecraft client, LocalPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu == player.inventoryMenu) {
            // 容器意外关闭，检查是否已有鱼竿
            if (hasFishingRod(player)) {
                sendOverlay(client, "§e[AutoFish+] §a已成功获取鱼竿！前往钓鱼点...");
                equipFishingRod(client);
                sendBaritoneGoto(client, 36, 59, -60);
                this.state = MacroState.NAVIGATING_TO_FISHING;
                this.timer = 0;
            } else {
                // 没拿到鱼竿，重新去交互
                this.state = MacroState.INTERACTING_WITH_NPC;
                this.timer = 0;
            }
            return;
        }

        // 等待界面数据同步（10 ticks后点击鱼竿）
        if (this.timer == 10) {
            int rodSlot = findRodSlotInContainer(menu);
            if (rodSlot != -1) {
                clickContainerSlot(client, rodSlot);
                sendOverlay(client, "§e[AutoFish+] §a已点击购买鱼竿！");
            } else {
                sendOverlay(client, "§e[AutoFish+] §c未在菜单中识别到鱼竿物品，重试中...");
            }
        }

        // 25 ticks 后关闭容器界面
        if (this.timer == 25) {
            player.closeContainer();
            client.setScreenAndShow(null);
        }

        // 35 ticks 后检查背包
        if (this.timer >= 35) {
            if (hasFishingRod(player)) {
                sendOverlay(client, "§e[AutoFish+] §a购买成功！前往钓鱼点 (36, 59, -60)...");
                equipFishingRod(client);
                sendBaritoneGoto(client, 36, 59, -60);
                this.state = MacroState.NAVIGATING_TO_FISHING;
                this.timer = 0;
            } else {
                sendOverlay(client, "§e[AutoFish+] §c未获取到鱼竿，重试交互 NPC...");
                this.state = MacroState.INTERACTING_WITH_NPC;
                this.timer = 0;
            }
        }
    }

    private void handleNavigatingToFishing(Minecraft client, LocalPlayer player) {
        double dx = player.getX() - FISH_X;
        double dz = player.getZ() - FISH_Z;
        double distSq = dx * dx + dz * dz;

        // 到达判定：水平距离 <= 1.4格，且Y轴误差 <= 2.0
        if (distSq <= 1.96 && Math.abs(player.getY() - FISH_Y) <= 2.0) {
            cancelBaritone(client);
            this.baseFishX = player.getX();
            this.baseFishZ = player.getZ();
            this.afkTimer = 0;
            this.noBobberTimer = 0;

            // 用户特别指定：超北方抛竿 (Yaw 180.0, Pitch -2.0 微仰平视)
            player.setYRot(180.0f);
            player.setXRot(28.0f);

            equipFishingRod(client);
            sendOverlay(client, "§e[AutoFish+] §a已就位钓鱼点！朝北方抛竿，启动自动钓鱼！");

            // 启动自动钓鱼
            ConfigManager.INSTANCE.getConfig().setEnabled(true);
            ConfigManager.INSTANCE.saveConfig();

            // 抛出第一杆
            MultiPlayerGameMode gameMode = client.gameMode;
            if (gameMode != null) {
                gameMode.useItem(player, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
            }

            this.state = MacroState.FISHING;
            this.timer = 0;
            return;
        }

        // 防卡死超时（45秒重发一次寻路指令）
        if (this.timer > 900) {
            sendBaritoneGoto(client, 36, 59, -60);
            this.timer = 0;
        }
    }

    private void handleFishingState(Minecraft client, LocalPlayer player) {
        // 如果正在进行神话鱼拉扯战斗，冻结防挂机位移和未抛竿重置，绝不打乱拉扯！
        if (MythicalFishHandler.INSTANCE.isFighting()) {
            this.afkTimer = 0;
            this.noBobberTimer = 0;
            player.setYRot(180.0f);
            player.setXRot(28.0f);
            return;
        }

        this.afkTimer++;

        // 钓鱼过程中始终锁定正北抛竿朝向 (Yaw 180.0, Pitch -2.0)
        player.setYRot(180.0f);
        player.setXRot(28.0f);

        // 每 20 ticks（1秒）巡检一次鱼竿与装备状态
        if (this.timer % 20 == 0) {
            // 确保手持鱼竿
            boolean holdingRod = player.getMainHandItem().getItem() == Items.FISHING_ROD || player.getMainHandItem().getItem() instanceof FishingRodItem;
            if (!holdingRod) {
                if (hasFishingRod(player)) {
                    equipFishingRod(client);
                } else {
                    // 背包里彻底没鱼竿了（爆竿！）
                    sendOverlay(client, "§e[AutoFish+] §c检测到鱼竿已损坏/爆竿！自动前往 NPC (22, 62, -36) 购买新鱼竿...");
                    ConfigManager.INSTANCE.getConfig().setEnabled(false);
                    sendBaritoneGoto(client, 22, 62, -36);
                    this.state = MacroState.NAVIGATING_TO_NPC;
                    this.timer = 0;
                    this.afkTimer = 0;
                    this.noBobberTimer = 0;
                    return;
                }
            }
        }

        // 鲁棒性保障：如果在钓鱼点位持续未抛竿（player.fishing == null），自动重置朝向正北并重新抛竿！
        if (player.fishing == null) {
            this.noBobberTimer++;
            // 持续约 2 秒 (40 ticks) 未检测到浮漂，立即重新朝正北校准并抛竿！
            if (this.noBobberTimer >= 40) {
                player.setYRot(180.0f);
                player.setXRot(28.0f);
                equipFishingRod(client);
                simulateRightClick(client, player);
                this.noBobberTimer = 0;
                sendOverlay(client, "§e[AutoFish+] §e检测到未抛竿，已校准正北重新抛竿！");
            }
        } else {
            this.noBobberTimer = 0;
        }

        // Anti-AFK 防挂机触发逻辑：
        // 挂机每约 35 秒 (700 ticks) 触发一次前后移动防挂机
        // 用户指定：不收杆（保持浮漂在水正常钓鱼），先往后退 (+Z) 1格，然后再往前移回到原地
        if (this.afkTimer >= 700) {
            sendOverlay(client, "§e[AutoFish+] §b[Anti-AFK] 触发防挂机移动：后退1格并返回原地...");
            player.setYRot(180.0f);
            player.setXRot(28.0f);
            setKeyDown(client, true);
            this.state = MacroState.AFK_STEP_BACK;
            this.timer = 0;
        }
    }

    private void handleAfkStepBack(Minecraft client, LocalPlayer player) {
        // 朝正北保持不变，按后退键 (S) 沿 +Z 轴安全退入陆地
        player.setYRot(180.0f);

        double targetBackZ = this.baseFishZ + 0.9;
        if (player.getZ() >= targetBackZ || this.timer >= 12) {
            setKeyDown(client, false);
            // 短暂停顿 3 ticks 消除惯性后前移回位
            if (this.timer >= 15) {
                player.setYRot(180.0f);
                setKeyUp(client, true);
                this.state = MacroState.AFK_STEP_FORWARD;
                this.timer = 0;
            }
        } else {
            setKeyDown(client, true);
        }
    }

    private void handleAfkStepForward(Minecraft client, LocalPlayer player) {
        player.setYRot(180.0f);

        // 前移回位判定：绝对防止掉水！
        // 目标原点为 baseFishZ，只要 Z <= baseFishZ 即已到位
        if (player.getZ() <= this.baseFishZ || this.timer >= 15) {
            // 立即松开前进键！
            setKeyUp(client, false);

            // 若因惯性超前 (Z < baseFishZ - 0.15)，后退微调
            if (player.getZ() < this.baseFishZ - 0.15) {
                setKeyDown(client, true);
            } else {
                setKeyDown(client, false);
            }

            // 停顿稳定 4 ticks
            if (this.timer >= 18) {
                setKeyUp(client, false);
                setKeyDown(client, false);

                // 重新锁定正北并微仰
                player.setYRot(180.0f);
                player.setXRot(28.0f);

                // 保持手持鱼竿，若浮漂不在水中则重新抛竿
                equipFishingRod(client);
                if (player.fishing == null) {
                    simulateRightClick(client, player);
                }

                this.state = MacroState.FISHING;
                this.timer = 0;
                this.afkTimer = 0;
                sendOverlay(client, "§e[AutoFish+] §a[Anti-AFK] 已安全回位，继续自动钓鱼！");
            }
        } else {
            setKeyUp(client, true);
        }
    }

    private static double getEntityCoord(Object e, String... methodNames) {
        if (e == null) return 0.0;
        for (String name : methodNames) {
            try {
                Method m = e.getClass().getMethod(name);
                return ((Number) m.invoke(e)).doubleValue();
            } catch (Throwable ignored) {
            }
        }
        for (Method m : e.getClass().getMethods()) {
            for (String name : methodNames) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    try {
                        return ((Number) m.invoke(e)).doubleValue();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return 0.0;
    }

    private void alignToNpc(Minecraft client, LocalPlayer player) {
        Object npc = findNearbyNpc(client, player, 4.5);
        if (npc != null) {
            try {
                double ex = getEntityCoord(npc, "method_23317", "getX");
                double ey = getEntityCoord(npc, "method_23318", "getY");
                double ez = getEntityCoord(npc, "method_23321", "getZ");
                double dx = ex - player.getX();
                double dy = (ey + 1.2) - (player.getY() + 1.62);
                double dz = ez - player.getZ();
                double distH = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, distH));
                player.setYRot(yaw);
                player.setXRot(pitch);
                return;
            } catch (Throwable ignored) {
            }
        }
        // 兜底朝向：Dock Master NPC 位于 (23, 62, -36)，玩家在 (22, 62, -36)，正对正东方向 (Yaw = -90.0f)
        player.setYRot(-90.0f);
        player.setXRot(5.0f);
    }

    private static Object findNearbyNpc(Minecraft client, LocalPlayer player, double maxDist) {
        if (client == null || client.level == null || player == null) return null;
        try {
            Object level = client.level;
            Method getEntities = null;
            for (Method m : level.getClass().getMethods()) {
                if ((m.getName().equals("method_18112") || m.getName().equals("entitiesForRendering") || m.getName().equals("getEntities"))
                        && m.getParameterCount() == 0 && Iterable.class.isAssignableFrom(m.getReturnType())) {
                    getEntities = m;
                    break;
                }
            }
            if (getEntities != null) {
                Iterable<?> entities = (Iterable<?>) getEntities.invoke(level);
                Object closest = null;
                double closestDistSq = maxDist * maxDist;
                for (Object e : entities) {
                    if (e == null || e == player) continue;
                    double ex = getEntityCoord(e, "method_23317", "getX");
                    double ey = getEntityCoord(e, "method_23318", "getY");
                    double ez = getEntityCoord(e, "method_23321", "getZ");
                    double dx = ex - player.getX();
                    double dy = ey - player.getY();
                    double dz = ez - player.getZ();
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = e;
                    }
                }
                return closest;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void interactWithNpc(Minecraft client, LocalPlayer player) {
        alignToNpc(client, player);

        // 尝试通过反射直接与检测到的 NPC 实体交互
        Object npc = findNearbyNpc(client, player, 4.5);
        if (npc != null && client.gameMode != null) {
            try {
                for (Method m : client.gameMode.getClass().getMethods()) {
                    if ((m.getName().equals("method_2905") || m.getName().equals("interact")) && m.getParameterCount() == 3) {
                        m.invoke(client.gameMode, player, npc, InteractionHand.MAIN_HAND);
                        player.swing(InteractionHand.MAIN_HAND);
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        simulateRightClick(client, player);
    }

    private void simulateRightClick(Minecraft client, LocalPlayer player) {
        if (client.options != null && client.options.keyUse != null) {
            client.options.keyUse.setDown(true);
            this.rightClickCooldown = 3;
        }
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode != null) {
            gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private int findRodSlotInContainer(AbstractContainerMenu menu) {
        if (menu == null || menu.slots == null) return -1;
        int maxSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < maxSlots; i++) {
            Slot slot = menu.slots.get(i);
            if (slot == null) continue;
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) continue;

            if (stack.getItem() == Items.FISHING_ROD || stack.getItem() instanceof FishingRodItem) {
                return slot.index;
            }

            try {
                String name = stack.getHoverName().getString().toLowerCase();
                if (name.contains("rod") || name.contains("竿") || name.contains("fish")) {
                    return slot.index;
                }
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }

    private void clickContainerSlot(Minecraft client, int slotIndex) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (player == null || gameMode == null || player.containerMenu == null) return;

        int containerId = player.containerMenu.containerId;
        try {
            Class<?> clickTypeClass = null;
            try {
                clickTypeClass = Class.forName("net.minecraft.class_1713");
            } catch (Throwable t) {
                try {
                    clickTypeClass = Class.forName("net.minecraft.world.inventory.ClickType");
                } catch (Throwable ignored) {}
            }
            if (clickTypeClass != null) {
                Object pickupType = Enum.valueOf((Class<Enum>) clickTypeClass, "PICKUP");
                for (Method m : gameMode.getClass().getMethods()) {
                    if ((m.getName().equals("method_2906") || m.getName().equals("handleInventoryMouseClick")) && m.getParameterCount() == 5) {
                        m.invoke(gameMode, containerId, slotIndex, 0, pickupType, player);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasFishingRod(LocalPlayer player) {
        if (player == null) return false;
        Inventory inv = player.getInventory();
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.isEmpty()) {
                if (stack.getItem() == Items.FISHING_ROD || stack.getItem() instanceof FishingRodItem) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean equipFishingRod(Minecraft client) {
        if (client == null || client.player == null) return false;
        LocalPlayer player = client.player;
        Inventory inv = player.getInventory();

        if (player.getMainHandItem().getItem() == Items.FISHING_ROD || player.getMainHandItem().getItem() instanceof FishingRodItem) {
            return true;
        }

        // 检查快捷栏 0~8
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.isEmpty()) {
                if (stack.getItem() == Items.FISHING_ROD || stack.getItem() instanceof FishingRodItem) {
                    inv.setSelectedSlot(i);
                    return true;
                }
            }
        }

        // 检查全背包并换到快捷栏
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.isEmpty()) {
                if (stack.getItem() == Items.FISHING_ROD || stack.getItem() instanceof FishingRodItem) {
                    AutoFishHandler.INSTANCE.swapToHotbar(client, i);
                    return true;
                }
            }
        }
        return false;
    }

    public static void sendBaritoneGoto(Minecraft client, int x, int y, int z) {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object primary = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);

            try {
                Object cmdMgr = primary.getClass().getMethod("getCommandManager").invoke(primary);
                Method exec = cmdMgr.getClass().getMethod("execute", String.class);
                exec.invoke(cmdMgr, "goto " + x + " " + y + " " + z);
                return;
            } catch (Throwable ignored) {
            }

            Object customGoal = primary.getClass().getMethod("getCustomGoalProcess").invoke(primary);
            Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Object goal = goalBlockClass.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
            customGoal.getClass().getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal")).invoke(customGoal, goal);
            return;
        } catch (Throwable ignored) {
        }

        if (client.player != null && client.player.connection != null) {
            client.player.connection.sendChat("#goto " + x + " " + y + " " + z);
        }
    }

    public static void cancelBaritone(Minecraft client) {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object primary = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);

            try {
                Object cmdMgr = primary.getClass().getMethod("getCommandManager").invoke(primary);
                Method exec = cmdMgr.getClass().getMethod("execute", String.class);
                exec.invoke(cmdMgr, "stop");
                return;
            } catch (Throwable ignored) {
            }

            Object pathing = primary.getClass().getMethod("getPathingBehavior").invoke(primary);
            pathing.getClass().getMethod("cancelEverything").invoke(pathing);
            return;
        } catch (Throwable ignored) {
        }

        if (client.player != null && client.player.connection != null) {
            client.player.connection.sendChat("#stop");
        }
    }

    public static void sendOverlay(Minecraft client, String text) {
        sendOverlay(client, Component.literal(text));
    }

    public static void sendOverlay(Minecraft client, Component component) {
        if (client == null || client.player == null || component == null) return;
        try {
            for (Method m : client.player.getClass().getMethods()) {
                if ((m.getName().equals("method_7353") || m.getName().equals("displayClientMessage") || m.getName().equals("sendMessage"))
                        && m.getParameterCount() == 2 && m.getParameterTypes()[1] == boolean.class) {
                    m.invoke(client.player, component, Boolean.TRUE);
                    return;
                }
            }
            for (Method m : client.player.getClass().getMethods()) {
                if ((m.getName().equals("sendOverlayMessage") || m.getName().equals("sendSystemMessage"))
                        && m.getParameterCount() == 1) {
                    m.invoke(client.player, component);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setKeyUp(Minecraft client, boolean down) {
        if (client == null || client.options == null) return;
        try {
            if (client.options.keyUp != null) {
                client.options.keyUp.setDown(down);
                return;
            }
        } catch (Throwable ignored) {}
        setKeyField(client.options, down, "keyUp", "field_1894", "forwardKey");
    }

    public static void setKeyDown(Minecraft client, boolean down) {
        if (client == null || client.options == null) return;
        try {
            if (client.options.keyDown != null) {
                client.options.keyDown.setDown(down);
                return;
            }
        } catch (Throwable ignored) {}
        setKeyField(client.options, down, "keyDown", "field_1881", "backKey");
    }

    private static void setKeyField(Object options, boolean down, String... fieldNames) {
        if (options == null) return;
        for (String name : fieldNames) {
            try {
                Field f = options.getClass().getField(name);
                Object key = f.get(options);
                if (key != null) {
                    for (Method m : key.getClass().getMethods()) {
                        if ((m.getName().equals("method_23481") || m.getName().equals("setDown"))
                                && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                            m.invoke(key, down);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}

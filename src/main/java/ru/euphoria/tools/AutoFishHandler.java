package ru.euphoria.tools;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Items;
import ru.euphoria.config.ConfigManager;

public final class AutoFishHandler {
    public static final AutoFishHandler INSTANCE = new AutoFishHandler();
    private static boolean wasHoldingRod;

    private AutoFishHandler() {
    }

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client == null) return;
        LocalPlayer player = client.player;
        if (player == null) return;

        boolean isHoldingRod = player.getMainHandItem().getItem() == Items.FISHING_ROD || player.getMainHandItem().getItem() instanceof FishingRodItem;
        if (wasHoldingRod && !isHoldingRod && ConfigManager.INSTANCE.getConfig().getReplaceRod()) {
            int slot = this.findFishingRodSlot(player);
            if (slot != -1) {
                this.swapToHotbar(client, slot);
            }
        }
        wasHoldingRod = isHoldingRod;
    }

    private int findFishingRodSlot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int slot = 0; slot < size; ++slot) {
            if (inventory.getItem(slot).getItem() == Items.FISHING_ROD || inventory.getItem(slot).getItem() instanceof FishingRodItem) {
                return slot;
            }
        }
        return -1;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void swapToHotbar(Minecraft client, int slot) {
        LocalPlayer player = client.player;
        if (player == null) return;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        int hotbarSlot = player.getInventory().getSelectedSlot();
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
                Object swapType = Enum.valueOf((Class<Enum>) clickTypeClass, "SWAP");
                for (Method m : gameMode.getClass().getMethods()) {
                    if ((m.getName().equals("method_2906") || m.getName().equals("handleInventoryMouseClick")) && m.getParameterCount() == 5) {
                        m.invoke(gameMode, player.containerMenu.containerId, slot, hotbarSlot, swapType, player);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> containerInputClass = Class.forName("net.minecraft.world.inventory.ContainerInput");
            Object swapInput = Enum.valueOf((Class<Enum>) containerInputClass, "SWAP");
            for (Method m : gameMode.getClass().getMethods()) {
                if (m.getName().equals("handleContainerInput") && m.getParameterCount() == 5) {
                    m.invoke(gameMode, player.containerMenu.containerId, slot, hotbarSlot, swapInput, player);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}

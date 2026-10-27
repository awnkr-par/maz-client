package com.maz.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Lightweight local inventory-capacity warning.
 *
 * Reads only the player's client-visible inventory and emits one local system message
 * when storage becomes completely full. No packets or inventory automation are used.
 */
public final class InventoryFullWarningsModule extends Module {
    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final int STORAGE_SLOTS = 36;

    private int tickCounter;
    private boolean wasFull;

    public InventoryFullWarningsModule() {
        super(
                "Inventory Full Warning",
                "Shows one local chat warning when all 36 main inventory slots become occupied.",
                ModuleCategory.UTILITY
        );
    }

    @Override
    public void onTick() {
        if (++tickCounter < SAMPLE_INTERVAL_TICKS) return;
        tickCounter = 0;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            wasFull = false;
            return;
        }

        Inventory inventory = client.player.getInventory();
        int slotCount = Math.min(STORAGE_SLOTS, inventory.getContainerSize());
        if (slotCount <= 0) {
            wasFull = false;
            return;
        }

        boolean full = true;
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                full = false;
                break;
            }
        }

        if (full && !wasFull) {
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[MazClient] Inventory full — all " + slotCount + " main slots are occupied.")
            );
        }
        wasFull = full;
    }

    @Override
    protected void onDisable() {
        tickCounter = 0;
        wasFull = false;
    }
}

package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InventorySpaceHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT_GOOD = 0xFF57F287;
    private static final int ACCENT_WARN = 0xFFFEE75C;
    private static final int ACCENT_LOW = 0xFFED4245;
    private static final int STORAGE_SLOTS = 36;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Inventory Space", 8, 580);

    private static Module inventorySpaceModule;
    private static String displayText = "Inventory: -- free | Partial: --";
    private static int displayWidth = 0;
    private static int accent = ACCENT_GOOD;

    private InventorySpaceHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (inventorySpaceModule == null || !inventorySpaceModule.isEnabled() || client.player == null) return;
        refresh(client);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (inventorySpaceModule == null || !inventorySpaceModule.isEnabled() || client.player == null) return;

        int x = LAYOUT.x(), y = LAYOUT.y(), alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(accent, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (inventorySpaceModule == null) inventorySpaceModule = MazClient.MODULE_MANAGER.getModule("Inventory Space");
    }

    private static void refresh(Minecraft client) {
        Inventory inventory = client.player.getInventory();
        int slotCount = Math.min(STORAGE_SLOTS, inventory.getContainerSize());
        int freeSlots = 0;
        int partialStacks = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) freeSlots++;
            else if (stack.getMaxStackSize() > 1 && stack.getCount() < stack.getMaxStackSize()) partialStacks++;
        }

        int occupiedPercent = slotCount > 0 ? Math.round(((slotCount - freeSlots) * 100.0F) / slotCount) : 0;
        String nextText = "Inventory: " + freeSlots + " free / " + slotCount
                + " | Partial: " + partialStacks + " | " + occupiedPercent + "% used";
        if (!nextText.equals(displayText) || displayWidth == 0) {
            displayText = nextText;
            displayWidth = client.font.width(displayText) + 12;
        }
        accent = freeSlots <= 3 ? ACCENT_LOW : freeSlots <= 9 ? ACCENT_WARN : ACCENT_GOOD;
    }

    private static int adaptiveTextColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        int channel = 255 - clamped;
        return 0xFF000000 | (channel << 16) | (channel << 8) | channel;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}

package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Passive client-side Totem of Undying counter.
 * Reads only the local player's already-loaded inventory and never sends packets,
 * moves items, equips totems, or automates any gameplay action.
 */
public final class TotemCounterHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final int MAIN_INVENTORY_SLOTS = 36;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Totem Counter", 8, 756);

    private static Module totemCounterModule;
    private static String displayText = "Totems: 0";
    private static int displayWidth;

    private TotemCounterHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (totemCounterModule == null || !totemCounterModule.isEnabled()) return;

        if (client.player == null) {
            updateDisplay(client, 0);
            return;
        }

        int total = 0;
        var inventory = client.player.getInventory();
        int mainSlots = Math.min(MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        for (int slot = 0; slot < mainSlots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING)) {
                total += stack.getCount();
            }
        }

        ItemStack offhand = client.player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.is(Items.TOTEM_OF_UNDYING)) {
            total += offhand.getCount();
        }

        updateDisplay(client, total);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (totemCounterModule == null || !totemCounterModule.isEnabled() || client.player == null) return;

        if (displayWidth == 0) {
            displayWidth = client.font.width(displayText) + 12;
        }

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (totemCounterModule == null) {
            totemCounterModule = MazClient.MODULE_MANAGER.getModule("Totem Counter");
        }
    }

    private static void updateDisplay(Minecraft client, int total) {
        String next = "Totems: " + total;
        if (!next.equals(displayText) || displayWidth == 0) {
            displayText = next;
            if (client != null && client.font != null) {
                displayWidth = client.font.width(displayText) + 12;
            }
        }
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

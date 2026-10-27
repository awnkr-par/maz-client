package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;


/**
 * Lightweight client-side offhand inventory counter.
 * Reads only already-loaded local player state and never sends packets or automates gameplay.
 */
public final class OffhandCounterHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Offhand Counter", 8, 646);

    private static Module offhandCounterModule;
    private static String displayText = "Offhand: --";
    private static int displayWidth = 0;

    private OffhandCounterHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (offhandCounterModule == null || !offhandCounterModule.isEnabled() || client.player == null) return;
        refresh(client);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (offhandCounterModule == null || !offhandCounterModule.isEnabled() || client.player == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (offhandCounterModule == null) offhandCounterModule = MazClient.MODULE_MANAGER.getModule("Offhand Counter");
    }

    private static void refresh(Minecraft client) {
        ItemStack offhand = client.player.getOffhandItem();
        String nextText;
        if (offhand.isEmpty()) {
            nextText = "Offhand: Empty";
        } else {
            int total = 0;
            for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = client.player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(offhand.getItem())) total += stack.getCount();
            }

            String name = offhand.getHoverName().getString();
            if (offhand.isDamageableItem()) {
                int max = offhand.getMaxDamage();
                int remaining = Math.max(0, max - offhand.getDamageValue());
                int percent = max > 0 ? Math.round((remaining * 100.0F) / max) : 0;
                nextText = "Offhand: " + name + " | Total: " + total + " | Durability: " + remaining + "/" + max + " (" + percent + "%)";
            } else {
                nextText = "Offhand: " + name + " | Total: " + total;
            }
        }

        if (!nextText.equals(displayText) || displayWidth == 0) {
            displayText = nextText;
            displayWidth = client.font.width(displayText) + 12;
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

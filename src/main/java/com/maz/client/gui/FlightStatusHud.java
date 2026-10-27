package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


/**
 * Lightweight elytra-flight telemetry from client state Minecraft already has loaded.
 * This HUD never sends packets, changes movement, or touches render/simulation distance.
 */
public final class FlightStatusHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT_GOOD = 0xFF57F287;
    private static final int ACCENT_WARN = 0xFFFEE75C;
    private static final int ACCENT_LOW = 0xFFED4245;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Flight Status", 8, 668);

    private static Module module;
    private static boolean wasFlying;
    private static String displayText = "Flight: --";
    private static int displayWidth;
    private static int accent = ACCENT_GOOD;

    private FlightStatusHud() {}

    public static void tick(Minecraft client, boolean scheduledRefresh) {
        resolveModule();
        if (module == null || !module.isEnabled() || client.player == null) {
            wasFlying = false;
            return;
        }

        boolean flying = client.player.isFallFlying();
        if (!flying) {
            wasFlying = false;
            return;
        }

        if (!wasFlying || scheduledRefresh || displayWidth == 0) refresh(client);
        wasFlying = true;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (module == null || !module.isEnabled() || client.player == null || !client.player.isFallFlying()) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(accent, alpha));
        graphics.drawString(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (module == null) module = MazClient.MODULE_MANAGER.getModule("Flight Status");
    }

    private static void refresh(Minecraft client) {
        double dx = client.player.getX() - client.player.xOld;
        double dy = client.player.getY() - client.player.yOld;
        double dz = client.player.getZ() - client.player.zOld;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;

        ItemStack chest = client.player.getItemBySlot(EquipmentSlot.CHEST);
        String durabilityText = "--";
        int durabilityPercent = 100;
        if (!chest.isEmpty() && chest.is(Items.ELYTRA) && chest.isDamageableItem()) {
            int max = chest.getMaxDamage();
            int remaining = Math.max(0, max - chest.getDamageValue());
            durabilityPercent = max > 0 ? Math.round((remaining * 100.0F) / max) : 0;
            durabilityText = remaining + "/" + max + " (" + durabilityPercent + "%)";
        }

        int rockets = 0;
        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(Items.FIREWORK_ROCKET)) rockets += stack.getCount();
        }

        String nextText = "Flight: " + oneDecimal(speed) + " b/s | Elytra: " + durabilityText + " | Rockets: " + rockets;
        if (!nextText.equals(displayText) || displayWidth == 0) {
            displayText = nextText;
            displayWidth = client.font.width(displayText) + 12;
        }
        accent = durabilityPercent <= 15 || rockets == 0
                ? ACCENT_LOW
                : durabilityPercent <= 30 || rockets <= 8 ? ACCENT_WARN : ACCENT_GOOD;
    }

    private static String oneDecimal(double value) {
        long tenths = Math.round(value * 10.0);
        long absolute = Math.abs(tenths);
        return (tenths < 0 ? "-" : "") + (absolute / 10) + "." + (absolute % 10);
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

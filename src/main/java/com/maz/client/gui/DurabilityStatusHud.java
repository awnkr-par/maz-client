package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class DurabilityStatusHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT_OK = 0xFF43A047;
    private static final int ACCENT_WARN = 0xFFFFB300;
    private static final int ACCENT_DANGER = 0xFFE53935;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Durability Status", 8, 580);

    private static Module durabilityStatusModule;
    private static String displayText = "Durability: no damageable gear";
    private static int displayWidth = 0;
    private static int accent = ACCENT_OK;

    private DurabilityStatusHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (durabilityStatusModule == null || !durabilityStatusModule.isEnabled() || client.player == null) return;
        refresh(client);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (durabilityStatusModule == null || !durabilityStatusModule.isEnabled() || client.player == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(accent, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (durabilityStatusModule == null) durabilityStatusModule = MazClient.MODULE_MANAGER.getModule("Durability Status");
    }

    private static void refresh(Minecraft client) {
        ItemStack weakest = ItemStack.EMPTY;
        int weakestPercent = 101;

        ItemStack held = client.player.getMainHandItem();
        if (held.isDamageableItem()) {
            weakest = held;
            weakestPercent = percentRemaining(held);
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = client.player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int percent = percentRemaining(stack);
                if (percent < weakestPercent) {
                    weakest = stack;
                    weakestPercent = percent;
                }
            }
        }

        String nextText;
        if (weakest.isEmpty()) {
            nextText = "Durability: no damageable gear";
            accent = ACCENT_OK;
        } else {
            int remaining = Math.max(0, weakest.getMaxDamage() - weakest.getDamageValue());
            nextText = "Durability: " + weakest.getHoverName().getString() + " " + remaining + "/" + weakest.getMaxDamage()
                    + " (" + weakestPercent + "%)";
            accent = weakestPercent <= 15 ? ACCENT_DANGER : weakestPercent <= 30 ? ACCENT_WARN : ACCENT_OK;
        }

        if (!nextText.equals(displayText) || displayWidth == 0) {
            displayText = nextText;
            displayWidth = client.font.width(displayText) + 12;
        }
    }

    private static int percentRemaining(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) return 100;
        int remaining = Math.max(0, max - stack.getDamageValue());
        return Math.round((remaining * 100.0F) / max);
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

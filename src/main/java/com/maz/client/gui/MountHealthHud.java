package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;


public final class MountHealthHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT_GOOD = 0xFF57F287;
    private static final int ACCENT_WARN = 0xFFFEE75C;
    private static final int ACCENT_LOW = 0xFFED4245;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Mount Health", 8, 646);

    private static Module module;
    private static LivingEntity lastMount;
    private static String displayText = "Mount: --";
    private static int displayWidth;
    private static int accent = ACCENT_GOOD;

    private MountHealthHud() {}

    public static void tick(Minecraft client, boolean scheduledRefresh) {
        resolveModule();
        if (module == null || !module.isEnabled() || client.player == null) {
            lastMount = null;
            return;
        }
        if (!(client.player.getVehicle() instanceof LivingEntity mount)) {
            lastMount = null;
            return;
        }

        if (mount != lastMount || scheduledRefresh || displayWidth == 0) {
            lastMount = mount;
            refresh(client, mount);
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (module == null || !module.isEnabled() || client.player == null || !(client.player.getVehicle() instanceof LivingEntity)) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(accent, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (module == null) module = MazClient.MODULE_MANAGER.getModule("Mount Health");
    }

    private static void refresh(Minecraft client, LivingEntity mount) {
        float health = Math.max(0.0F, mount.getHealth());
        float maxHealth = Math.max(0.0F, mount.getMaxHealth());
        int percent = maxHealth > 0.0F ? Math.round((health * 100.0F) / maxHealth) : 0;
        String nextText = mount.getName().getString() + ": " + oneDecimal(health) + " / " + oneDecimal(maxHealth) + " HP (" + percent + "%)";
        if (!nextText.equals(displayText) || displayWidth == 0) {
            displayText = nextText;
            displayWidth = client.font.width(displayText) + 12;
        }
        accent = percent <= 25 ? ACCENT_LOW : percent <= 50 ? ACCENT_WARN : ACCENT_GOOD;
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

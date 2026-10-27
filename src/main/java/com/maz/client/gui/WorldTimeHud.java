package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class WorldTimeHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final String[] MOON_PHASES = {
            "Full Moon",
            "Waning Gibbous",
            "Third Quarter",
            "Waning Crescent",
            "New Moon",
            "Waxing Crescent",
            "First Quarter",
            "Waxing Gibbous"
    };
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("World Time", 8, 558);

    private static Module worldTimeModule;
    private static String timeText = "World Time: Day -- | --:-- | Moon: --";
    private static int timeWidth;

    private WorldTimeHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (worldTimeModule == null || !worldTimeModule.isEnabled() || client.level == null) return;

        long dayTime = client.level.getDayTime();
        long elapsedDays = Math.floorDiv(dayTime, 24000L);
        long day = elapsedDays + 1L;
        long tickOfDay = Math.floorMod(dayTime, 24000L);
        int totalMinutes = (int) ((tickOfDay * 1440L) / 24000L);
        totalMinutes = (totalMinutes + 360) % 1440;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        String moonPhase = MOON_PHASES[(int) Math.floorMod(elapsedDays, MOON_PHASES.length)];
        String nextText = "World Time: Day " + day + " | " + (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute + " | Moon: " + moonPhase;
        if (!nextText.equals(timeText) || timeWidth == 0) {
            timeText = nextText;
            timeWidth = client.font.width(timeText) + 12;
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (worldTimeModule == null || !worldTimeModule.isEnabled() || client.level == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + timeWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.drawString(client.font, timeText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (worldTimeModule == null) worldTimeModule = MazClient.MODULE_MANAGER.getModule("World Time");
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

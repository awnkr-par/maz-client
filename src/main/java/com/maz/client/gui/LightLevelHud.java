package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

public final class LightLevelHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Light Level", 8, 536);
    private static Module lightLevelModule;
    private static String lightText = "Light: Block -- | Sky --";
    private static int lightWidth;

    private LightLevelHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (lightLevelModule == null || !lightLevelModule.isEnabled() || client.level == null || client.player == null) return;

        BlockPos pos = client.player.blockPosition();
        int block = client.level.getBrightness(LightLayer.BLOCK, pos);
        int sky = client.level.getBrightness(LightLayer.SKY, pos);
        String nextText = "Light: Block " + block + " | Sky " + sky;
        if (!nextText.equals(lightText) || lightWidth == 0) {
            lightText = nextText;
            lightWidth = client.font.width(lightText) + 12;
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (lightLevelModule == null || !lightLevelModule.isEnabled() || client.level == null || client.player == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + lightWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, lightText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (lightLevelModule == null) lightLevelModule = MazClient.MODULE_MANAGER.getModule("Light Level");
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

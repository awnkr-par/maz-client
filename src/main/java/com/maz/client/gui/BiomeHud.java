package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

public final class BiomeHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Biome", 8, 492);
    private static Module biomeModule;
    private static String biomeText = "Biome: --";
    private static int biomeWidth;

    private BiomeHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (biomeModule == null || !biomeModule.isEnabled() || client.player == null || client.level == null) return;

        String nextText = "Biome: " + client.level.getBiome(client.player.blockPosition())
                .unwrapKey()
                .map(key -> titleCase(key.identifier().getPath()))
                .orElse("Unknown");
        if (!nextText.equals(biomeText) || biomeWidth == 0) {
            biomeText = nextText;
            biomeWidth = client.font.width(biomeText) + 12;
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (biomeModule == null || !biomeModule.isEnabled() || client.player == null || client.level == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + biomeWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, biomeText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (biomeModule == null) biomeModule = MazClient.MODULE_MANAGER.getModule("Biome");
    }

    private static String titleCase(String path) {
        String[] parts = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.isEmpty() ? "Unknown" : result.toString();
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

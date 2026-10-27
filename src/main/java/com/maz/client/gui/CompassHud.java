package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class CompassHud implements ClientModInitializer {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final int MUTED = 0xFF64748B;
    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private static final int[] DIRECTION_WIDTHS = new int[DIRECTIONS.length];
    private static final int[] CENTER_WIDTHS = new int[360];
    private static Module compassModule;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MazClient.MOD_ID, "compass_hud"),
                CompassHud::render
        );
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (compassModule == null) compassModule = MazClient.MODULE_MANAGER.getModule("Compass");
        if (compassModule == null || !compassModule.isEnabled() || client.player == null) return;

        float yaw = client.player.getYRot();
        float normalized = ((yaw % 360.0F) + 360.0F) % 360.0F;
        int degrees = Math.round(normalized) % 360;
        int directionIndex = directionIndexFor(normalized);
        int leftIndex = directionIndexFor(normalized - 45.0F);
        int rightIndex = directionIndexFor(normalized + 45.0F);
        String direction = DIRECTIONS[directionIndex];
        String left = DIRECTIONS[leftIndex];
        String right = DIRECTIONS[rightIndex];

        HudLayout.Position p = HudLayout.getPosition("Compass", 8, 214);
        int alpha = HudLayout.getOpacity("Compass");
        int width = 152;
        int height = 24;
        int x = p.x();
        int y = p.y();

        graphics.fill(x, y, x + width, y + height, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + height, withAlpha(ACCENT, alpha));
        graphics.text(client.font, left, x + 12, y + 8, withAlpha(MUTED, 255), false);

        String center = direction + "  " + degrees + "°";
        int centerWidth = CENTER_WIDTHS[degrees];
        if (centerWidth == 0) {
            centerWidth = client.font.width(center);
            CENTER_WIDTHS[degrees] = centerWidth;
        }
        int centerX = x + width / 2 - centerWidth / 2;
        graphics.text(client.font, center, centerX, y + 8, adaptiveTextColor(alpha), false);

        int rightWidth = DIRECTION_WIDTHS[rightIndex];
        if (rightWidth == 0) {
            rightWidth = client.font.width(right);
            DIRECTION_WIDTHS[rightIndex] = rightWidth;
        }
        int rightX = x + width - 12 - rightWidth;
        graphics.text(client.font, right, rightX, y + 8, withAlpha(MUTED, 255), false);
    }

    private static int directionIndexFor(float yaw) {
        float normalized = ((yaw % 360.0F) + 360.0F) % 360.0F;
        return Math.round(normalized / 45.0F) & 7;
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

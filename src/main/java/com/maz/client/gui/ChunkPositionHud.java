package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

/**
 * Lightweight local chunk-position HUD derived only from the player's client-side position.
 * It never sends packets, changes movement, or touches render/simulation distance.
 */
public final class ChunkPositionHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Chunk Position", 8, 646);

    private static Module chunkPositionModule;
    private static String displayText = "Chunk: -- -- | In-chunk: -- --";
    private static int displayWidth;

    private ChunkPositionHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (chunkPositionModule == null || !chunkPositionModule.isEnabled() || client.player == null) return;
        refresh(client);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (chunkPositionModule == null || !chunkPositionModule.isEnabled() || client.player == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (chunkPositionModule == null) chunkPositionModule = MazClient.MODULE_MANAGER.getModule("Chunk Position");
    }

    private static void refresh(Minecraft client) {
        BlockPos pos = client.player.blockPosition();
        int chunkX = Math.floorDiv(pos.getX(), 16);
        int chunkZ = Math.floorDiv(pos.getZ(), 16);
        int inChunkX = Math.floorMod(pos.getX(), 16);
        int inChunkZ = Math.floorMod(pos.getZ(), 16);
        String nextText = "Chunk: " + chunkX + " " + chunkZ + " | In-chunk: " + inChunkX + " " + inChunkZ;
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

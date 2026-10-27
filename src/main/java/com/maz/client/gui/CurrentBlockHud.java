package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public final class CurrentBlockHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Current Block", 8, 624);

    private static Module currentBlockModule;
    private static String displayText = "Block: --";
    private static int displayWidth;
    private static BlockPos cachedTargetPos;
    private static Block cachedTargetBlock;

    private CurrentBlockHud() {}

    public static void tick(Minecraft client) {
        resolveModule();
        if (currentBlockModule == null || !currentBlockModule.isEnabled()) return;
        if (client.level == null) {
            resetTargetCache();
            return;
        }
        refresh(client);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (currentBlockModule == null || !currentBlockModule.isEnabled() || client.level == null) return;

        int x = LAYOUT.x(), y = LAYOUT.y(), alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void resolveModule() {
        if (currentBlockModule == null) currentBlockModule = MazClient.MODULE_MANAGER.getModule("Current Block");
    }

    private static void refresh(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            cachedTargetPos = null;
            cachedTargetBlock = null;
            updateDisplay(client, "Block: --");
            return;
        }

        BlockPos pos = hit.getBlockPos();
        Block block = client.level.getBlockState(pos).getBlock();
        if (displayWidth != 0 && pos.equals(cachedTargetPos) && block == cachedTargetBlock) return;

        cachedTargetPos = pos.immutable();
        cachedTargetBlock = block;
        updateDisplay(client, "Block: " + block.getName().getString() + " | " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    private static void updateDisplay(Minecraft client, String nextText) {
        if (!nextText.equals(displayText) || displayWidth == 0) {
            displayText = nextText;
            displayWidth = client.font.width(displayText) + 12;
        }
    }

    private static void resetTargetCache() {
        cachedTargetPos = null;
        cachedTargetBlock = null;
        displayText = "Block: --";
        displayWidth = 0;
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

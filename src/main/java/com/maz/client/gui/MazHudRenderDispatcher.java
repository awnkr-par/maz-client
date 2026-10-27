package com.maz.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Single Fabric HUD entry point for all MazClient overlays.
 *
 * Keeping one registry callback avoids per-frame registry dispatch through a separate
 * callback for every specialized HUD. Individual HUDs still own their rendering and
 * enabled-state checks, so behavior and module controls remain unchanged.
 */
public final class MazHudRenderDispatcher {
    private MazHudRenderDispatcher() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        MazHud.render(graphics, deltaTracker);
        FrameStatsHud.render(graphics, deltaTracker);
        BiomeHud.render(graphics, deltaTracker);
        DimensionHud.render(graphics, deltaTracker);
        LightLevelHud.render(graphics, deltaTracker);
        WorldTimeHud.render(graphics, deltaTracker);
        DurabilityStatusHud.render(graphics, deltaTracker);
        XpProgressHud.render(graphics, deltaTracker);
        LastDeathHud.render(graphics, deltaTracker);
        InventorySpaceHud.render(graphics, deltaTracker);
        OffhandCounterHud.render(graphics, deltaTracker);
        RecentGainsHud.render(graphics, deltaTracker);
        MountHealthHud.render(graphics, deltaTracker);
        FlightStatusHud.render(graphics, deltaTracker);
        CurrentBlockHud.render(graphics, deltaTracker);
        ChunkPositionHud.render(graphics, deltaTracker);
    }
}

package com.maz.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LowAirWarningModule extends Module {
    private static final int SAMPLE_INTERVAL_TICKS = 10;
    private static final int WARNING_AIR_TICKS = 100;
    private static final int CRITICAL_AIR_TICKS = 40;

    private int tickCounter;
    private Severity lastSeverity = Severity.NORMAL;

    public LowAirWarningModule() {
        super(
                "Low Air Warning",
                "Shows local chat warnings when your remaining air drops to about 5 and 2 seconds.",
                ModuleCategory.UTILITY
        );
    }

    @Override
    public void onTick() {
        if (++tickCounter < SAMPLE_INTERVAL_TICKS) return;
        tickCounter = 0;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.isDeadOrDying()) {
            lastSeverity = Severity.NORMAL;
            return;
        }

        int air = client.player.getAirSupply();
        int maxAir = client.player.getMaxAirSupply();

        if (maxAir <= 0 || air >= maxAir) {
            lastSeverity = Severity.NORMAL;
            return;
        }

        Severity severity = air <= CRITICAL_AIR_TICKS
                ? Severity.CRITICAL
                : air <= WARNING_AIR_TICKS ? Severity.WARNING : Severity.NORMAL;

        if (severity.ordinal() > lastSeverity.ordinal()) {
            int secondsRemaining = Math.max(0, (air + 19) / 20);
            String label = severity == Severity.CRITICAL ? "Critical air" : "Low air";
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[MazClient] " + label + " - about "
                            + secondsRemaining + "s remaining.")
            );
        }

        lastSeverity = severity;
    }

    @Override
    protected void onDisable() {
        tickCounter = 0;
        lastSeverity = Severity.NORMAL;
    }

    private enum Severity {
        NORMAL,
        WARNING,
        CRITICAL
    }
}

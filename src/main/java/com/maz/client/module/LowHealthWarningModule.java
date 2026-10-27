package com.maz.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LowHealthWarningModule extends Module {
    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final float WARNING_FRACTION = 0.30F;
    private static final float CRITICAL_FRACTION = 0.15F;

    private int tickCounter;
    private Severity lastSeverity = Severity.NORMAL;

    public LowHealthWarningModule() {
        super(
                "Low Health Warning",
                "Shows local chat warnings when your health drops below 30% and 15%.",
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

        float maxHealth = client.player.getMaxHealth();
        if (maxHealth <= 0.0F) {
            lastSeverity = Severity.NORMAL;
            return;
        }

        float health = Math.max(0.0F, client.player.getHealth());
        float fraction = health / maxHealth;
        Severity severity = fraction <= CRITICAL_FRACTION
                ? Severity.CRITICAL
                : fraction <= WARNING_FRACTION ? Severity.WARNING : Severity.NORMAL;

        if (severity.ordinal() > lastSeverity.ordinal()) {
            int percent = Math.round(fraction * 100.0F);
            String label = severity == Severity.CRITICAL ? "Critical health" : "Low health";
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[MazClient] " + label + " - "
                            + Math.round(health * 10.0F) / 10.0F + "/"
                            + Math.round(maxHealth * 10.0F) / 10.0F
                            + " (" + percent + "%).")
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

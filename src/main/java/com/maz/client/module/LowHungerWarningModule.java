package com.maz.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LowHungerWarningModule extends Module {
    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final int WARNING_FOOD_LEVEL = 6;
    private static final int CRITICAL_FOOD_LEVEL = 2;

    private int tickCounter;
    private Severity lastSeverity = Severity.NORMAL;

    public LowHungerWarningModule() {
        super(
                "Low Hunger Warning",
                "Shows local chat warnings when your hunger drops to 6 and 2 shanks.",
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

        int foodLevel = client.player.getFoodData().getFoodLevel();
        Severity severity = foodLevel <= CRITICAL_FOOD_LEVEL
                ? Severity.CRITICAL
                : foodLevel <= WARNING_FOOD_LEVEL ? Severity.WARNING : Severity.NORMAL;

        if (severity.ordinal() > lastSeverity.ordinal()) {
            String label = severity == Severity.CRITICAL ? "Critical hunger" : "Low hunger";
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[MazClient] " + label + " - " + foodLevel + "/20. Time to eat.")
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

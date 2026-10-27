package com.maz.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Low-overhead local durability alerts for the held tool and equipped armor.
 *
 * This reads only client-visible ItemStack state and emits local system messages.
 * It does not send packets, automate inventory actions, repair gear, or change gameplay.
 */
public final class DurabilityWarningsModule extends Module {
    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final int LOW_PERCENT = 10;
    private static final int CRITICAL_PERCENT = 3;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private int tickCounter;
    private int mainHandSeverity;
    private final int[] armorSeverity = new int[ARMOR_SLOTS.length];

    public DurabilityWarningsModule() {
        super(
                "Durability Warnings",
                "Shows local chat warnings when your held tool or equipped armor drops to 10% or 3% durability.",
                ModuleCategory.UTILITY
        );
    }

    @Override
    public void onTick() {
        if (++tickCounter < SAMPLE_INTERVAL_TICKS) return;
        tickCounter = 0;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            clearState();
            return;
        }

        mainHandSeverity = checkStack(client, client.player.getMainHandItem(), "Held item", mainHandSeverity);
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            armorSeverity[i] = checkStack(client, client.player.getItemBySlot(slot), slotLabel(slot), armorSeverity[i]);
        }
    }

    private static int checkStack(Minecraft client, ItemStack stack, String slotLabel, int previousSeverity) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 0;

        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        int percent = Math.max(0, Math.min(100, Math.round((remaining * 100.0F) / stack.getMaxDamage())));
        int severity = percent <= CRITICAL_PERCENT ? 2 : percent <= LOW_PERCENT ? 1 : 0;

        if (severity > previousSeverity) {
            String level = severity == 2 ? "CRITICAL" : "LOW";
            String message = "[MazClient] " + level + " durability: " + slotLabel + " — "
                    + stack.getHoverName().getString() + " " + remaining + "/" + stack.getMaxDamage()
                    + " (" + percent + "%)";
            client.player.displayClientMessage(Component.literal(message), false);
        }
        return severity;
    }

    private static String slotLabel(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "Helmet";
            case CHEST -> "Chestplate";
            case LEGS -> "Leggings";
            case FEET -> "Boots";
            default -> "Armor";
        };
    }

    @Override
    protected void onDisable() {
        clearState();
    }

    private void clearState() {
        tickCounter = 0;
        mainHandSeverity = 0;
        for (int i = 0; i < armorSeverity.length; i++) armorSeverity[i] = 0;
    }
}

package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight local inventory-gain feed.
 * Samples aggregate counts from already-loaded inventory state only.
 */
public final class RecentGainsHud {
    private static final long ENTRY_LIFETIME_MS = 6_000L;
    private static final int MAX_ENTRIES = 4;
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final String EMPTY_TEXT = "Recent gains: none";
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Recent Gains", 8, 668);

    private static final Map<Item, Integer> previousTotals = new HashMap<>();
    private static final Map<Item, Integer> currentTotals = new HashMap<>();
    private static final Map<Item, String> currentDisplayNames = new HashMap<>();
    private static final Deque<GainEntry> entries = new ArrayDeque<>();

    private static Module recentGainsModule;
    private static UUID activePlayerId;
    private static boolean layoutDirty = true;
    private static int cachedWidth;

    private RecentGainsHud() {}

    public static void tick(Minecraft client, long now) {
        resolveModule();
        if (client.player == null) {
            reset();
            return;
        }
        if (recentGainsModule == null || !recentGainsModule.isEnabled()) return;

        refresh(client, now);
        discardExpired(now);
        refreshCachedWidth(client);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModule();
        if (recentGainsModule == null || !recentGainsModule.isEnabled() || client.player == null) return;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        int lines = Math.max(1, entries.size());
        int height = 10 + lines * 10;

        graphics.fill(x, y, x + cachedWidth, y + height, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + height, withAlpha(ACCENT, alpha));

        int textColor = adaptiveTextColor(alpha);
        if (entries.isEmpty()) {
            graphics.text(client.font, EMPTY_TEXT, x + 7, y + 7, textColor, false);
            return;
        }

        int textY = y + 7;
        for (GainEntry entry : entries) {
            graphics.text(client.font, entry.text(), x + 7, textY, textColor, false);
            textY += 10;
        }
    }

    private static void resolveModule() {
        if (recentGainsModule == null) recentGainsModule = MazClient.MODULE_MANAGER.getModule("Recent Gains");
    }

    private static void refresh(Minecraft client, long now) {
        UUID playerId = client.player.getUUID();
        currentTotals.clear();
        currentDisplayNames.clear();

        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            currentTotals.merge(item, stack.getCount(), Integer::sum);
            currentDisplayNames.putIfAbsent(item, stack.getHoverName().getString());
        }

        if (!playerId.equals(activePlayerId)) {
            activePlayerId = playerId;
            previousTotals.clear();
            previousTotals.putAll(currentTotals);
            if (!entries.isEmpty()) {
                entries.clear();
                layoutDirty = true;
            }
            return;
        }

        for (Map.Entry<Item, Integer> current : currentTotals.entrySet()) {
            int before = previousTotals.getOrDefault(current.getKey(), 0);
            int gained = current.getValue() - before;
            if (gained > 0) recordGain(currentDisplayNames.getOrDefault(current.getKey(), "Item"), gained, now);
        }

        previousTotals.clear();
        previousTotals.putAll(currentTotals);
    }

    private static void recordGain(String name, int amount, long now) {
        GainEntry newest = entries.peekFirst();
        if (newest != null && newest.name().equals(name) && now - newest.timestampMs() <= 1_000L) {
            entries.removeFirst();
            entries.addFirst(GainEntry.create(name, newest.amount() + amount, now));
        } else {
            entries.addFirst(GainEntry.create(name, amount, now));
        }
        while (entries.size() > MAX_ENTRIES) entries.removeLast();
        layoutDirty = true;
    }

    private static void discardExpired(long now) {
        boolean changed = false;
        while (!entries.isEmpty() && now - entries.peekLast().timestampMs() > ENTRY_LIFETIME_MS) {
            entries.removeLast();
            changed = true;
        }
        if (changed) layoutDirty = true;
    }

    private static void refreshCachedWidth(Minecraft client) {
        if (!layoutDirty) return;
        int width = client.font.width(EMPTY_TEXT) + 12;
        for (GainEntry entry : entries) width = Math.max(width, client.font.width(entry.text()) + 12);
        cachedWidth = width;
        layoutDirty = false;
    }

    private static void reset() {
        activePlayerId = null;
        previousTotals.clear();
        currentTotals.clear();
        currentDisplayNames.clear();
        entries.clear();
        cachedWidth = 0;
        layoutDirty = true;
    }

    private static int adaptiveTextColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        int channel = 255 - clamped;
        return 0xFF000000 | (channel << 16) | (channel << 8) | channel;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private record GainEntry(String name, int amount, long timestampMs, String text) {
        private static GainEntry create(String name, int amount, long timestampMs) {
            return new GainEntry(name, amount, timestampMs, "+" + amount + " " + name);
        }
    }
}

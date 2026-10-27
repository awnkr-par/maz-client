package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

public final class LastDeathHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFFE74C3C;
    private static final Path STATE_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("maz-client-last-death.properties");
    private static final HudLayout.Binding LAYOUT = HudLayout.bind("Last Death", 8, 624);

    private static Module module;
    private static boolean stateLoaded;
    private static boolean wasDead;
    private static boolean hasDeath;
    private static int deathX;
    private static int deathY;
    private static int deathZ;
    private static String deathDimension = "Unknown";
    private static String displayText = "Last Death: --";
    private static int displayWidth;

    private LastDeathHud() {}

    public static void tick(Minecraft client) {
        loadStateOnce();

        if (client.player == null || client.level == null) {
            wasDead = false;
            return;
        }

        boolean dead = client.player.isDeadOrDying();
        if (dead && !wasDead) {
            deathX = (int) Math.floor(client.player.getX());
            deathY = (int) Math.floor(client.player.getY());
            deathZ = (int) Math.floor(client.player.getZ());
            deathDimension = titleCase(client.level.dimension().identifier().getPath());
            refreshDisplayText(client);
            hasDeath = true;
            saveState();
        }
        wasDead = dead;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        loadStateOnce();

        if (module == null) module = MazClient.MODULE_MANAGER.getModule("Last Death");
        if (module == null || !module.isEnabled() || !hasDeath || client.player == null) return;

        if (displayWidth == 0) displayWidth = client.font.width(displayText) + 12;

        int x = LAYOUT.x();
        int y = LAYOUT.y();
        int alpha = LAYOUT.opacity();
        graphics.fill(x, y, x + displayWidth, y + 18, withAlpha(BACKGROUND, alpha));
        graphics.fill(x, y, x + 3, y + 18, withAlpha(ACCENT, alpha));
        graphics.text(client.font, displayText, x + 7, y + 6, adaptiveTextColor(alpha), false);
    }

    private static void loadStateOnce() {
        if (stateLoaded) return;
        stateLoaded = true;

        if (!Files.isRegularFile(STATE_PATH)) return;

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(STATE_PATH)) {
            properties.load(input);

            int x = Integer.parseInt(properties.getProperty("x"));
            int y = Integer.parseInt(properties.getProperty("y"));
            int z = Integer.parseInt(properties.getProperty("z"));
            String dimension = properties.getProperty("dimension");
            if (dimension == null || dimension.isBlank()) return;

            deathX = x;
            deathY = y;
            deathZ = z;
            deathDimension = dimension;
            hasDeath = true;
            refreshDisplayText(null);
        } catch (IOException | NumberFormatException | NullPointerException exception) {
            System.err.println("MazClient: failed to load last death state: " + exception.getMessage());
        }
    }

    private static void saveState() {
        Properties properties = new Properties();
        properties.setProperty("x", Integer.toString(deathX));
        properties.setProperty("y", Integer.toString(deathY));
        properties.setProperty("z", Integer.toString(deathZ));
        properties.setProperty("dimension", deathDimension);

        Path tempPath = STATE_PATH.resolveSibling(STATE_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(STATE_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(tempPath)) {
                properties.store(output, "MazClient last death location");
            }
            try {
                Files.move(tempPath, STATE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(tempPath, STATE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("MazClient: failed to save last death state: " + exception.getMessage());
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
        }
    }

    private static void refreshDisplayText(Minecraft client) {
        displayText = "Last Death: " + deathX + ", " + deathY + ", " + deathZ + " | " + deathDimension;
        displayWidth = client == null ? 0 : client.font.width(displayText) + 12;
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

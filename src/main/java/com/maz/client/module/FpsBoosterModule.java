package com.maz.client.module;

import com.maz.client.MazClient;
import com.maz.client.config.ClientConfig;
import com.maz.client.gui.FpsBoosterScreen;
import net.minecraft.client.Minecraft;

import java.util.Properties;

/**
 * Applies configurable client-side performance reductions while enabled, then
 * restores the exact affected player settings that were active before enabling it.
 * Render distance and simulation distance are never modified by this module.
 */
public class FpsBoosterModule extends Module {

    private Double previousEntityDistanceScaling;
    private Boolean previousEntityShadows;

    private int entityDistancePercent = 50;
    private int particleKeepEvery = 16;
    private boolean entityShadows = false;

    public FpsBoosterModule() {
        super("FPS Booster", ModuleCategory.PERFORMANCE);
    }

    @Override
    public void toggle() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new FpsBoosterScreen(client.screen));
    }

    public int getEntityDistancePercent() {
        return entityDistancePercent;
    }

    public int getParticleKeepEvery() {
        return particleKeepEvery;
    }

    public boolean getEntityShadows() {
        return entityShadows;
    }

    public void setEntityDistancePercent(int value) {
        entityDistancePercent = clamp(value, 50, 150);
        settingsChanged();
    }

    public void setParticleKeepEvery(int value) {
        particleKeepEvery = clamp(value, 1, 32);
        settingsChanged();
    }

    public void setEntityShadows(boolean value) {
        entityShadows = value;
        settingsChanged();
    }

    public void applyMaxFpsProfile() {
        entityDistancePercent = 50;
        particleKeepEvery = 32;
        entityShadows = false;

        if (!isEnabled()) {
            setEnabled(true);
        } else {
            applyProfile();
            ClientConfig.save(MazClient.MODULE_MANAGER);
        }
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();

        previousEntityDistanceScaling = client.options.entityDistanceScaling().get();
        previousEntityShadows = client.options.entityShadows().get();

        applyProfile();
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();

        if (previousEntityDistanceScaling != null) {
            client.options.entityDistanceScaling().set(previousEntityDistanceScaling);
        }
        if (previousEntityShadows != null) {
            client.options.entityShadows().set(previousEntityShadows);
        }

        previousEntityDistanceScaling = null;
        previousEntityShadows = null;
    }

    private void applyProfile() {
        if (!isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        double baseEntity = previousEntityDistanceScaling != null
                ? previousEntityDistanceScaling
                : client.options.entityDistanceScaling().get();

        client.options.entityDistanceScaling().set(Math.min(baseEntity, entityDistancePercent / 100.0D));
        client.options.entityShadows().set(entityShadows);
    }

    private void settingsChanged() {
        applyProfile();
        ClientConfig.save(MazClient.MODULE_MANAGER);
    }

    @Override
    public void loadConfig(Properties properties) {
        String prefix = "fpsBooster.";
        entityDistancePercent = parseInt(properties.getProperty(prefix + "entityDistancePercent"), entityDistancePercent, 50, 150);
        particleKeepEvery = parseInt(properties.getProperty(prefix + "particleKeepEvery"), particleKeepEvery, 1, 32);
        entityShadows = Boolean.parseBoolean(properties.getProperty(prefix + "entityShadows", Boolean.toString(entityShadows)));
    }

    @Override
    public void saveConfig(Properties properties) {
        String prefix = "fpsBooster.";
        properties.remove(prefix + "preset");
        properties.remove(prefix + "simulationDistance");
        properties.setProperty(prefix + "entityDistancePercent", Integer.toString(entityDistancePercent));
        properties.setProperty(prefix + "particleKeepEvery", Integer.toString(particleKeepEvery));
        properties.setProperty(prefix + "entityShadows", Boolean.toString(entityShadows));
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            return clamp(Integer.parseInt(value), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

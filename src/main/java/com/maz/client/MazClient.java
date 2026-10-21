package com.maz.client;

import com.maz.client.config.ClientConfig;
import com.maz.client.gui.LastDeathHud;
import com.maz.client.gui.MazHud;
import com.maz.client.gui.MazHudRenderDispatcher;
import com.maz.client.gui.MazMenuScreen;
import com.maz.client.gui.SpecializedHudScheduler;
import com.maz.client.module.AdvancedTooltipsModule;
import com.maz.client.module.ClearChatModule;
import com.maz.client.module.ClockModule;
import com.maz.client.module.CoordinatesModule;
import com.maz.client.module.CpsModule;
import com.maz.client.module.DirectionModule;
import com.maz.client.module.DurabilityWarningsModule;
import com.maz.client.module.FpsBoosterModule;
import com.maz.client.module.FpsModule;
import com.maz.client.module.FullbrightModule;
import com.maz.client.module.InventoryFullWarningsModule;
import com.maz.client.module.KeystrokesModule;
import com.maz.client.module.LowAirWarningModule;
import com.maz.client.module.LowHealthWarningModule;
import com.maz.client.module.LowHungerWarningModule;
import com.maz.client.module.MemoryModule;
import com.maz.client.module.ModuleCategory;
import com.maz.client.module.ModuleHotkeys;
import com.maz.client.module.ModuleManager;
import com.maz.client.module.NoDynamicFovModule;
import com.maz.client.module.NoHurtCamModule;
import com.maz.client.module.NoRainModule;
import com.maz.client.module.PingModule;
import com.maz.client.module.PotCounterModule;
import com.maz.client.module.SessionTimerModule;
import com.maz.client.module.SimpleModule;
import com.maz.client.module.SmoothCameraModule;
import com.maz.client.module.SpeedModule;
import com.maz.client.module.ToggleSneakModule;
import com.maz.client.module.ToggleSprintModule;
import com.maz.client.module.ZoomModule;
import com.maz.client.mixin.ParticleGroupMixin;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

public class MazClient implements ClientModInitializer {

    public static final String MOD_ID = "maz-client";

    public static final ModuleManager MODULE_MANAGER = new ModuleManager();
    public static final long SESSION_START_MILLIS = System.currentTimeMillis();

    private static KeyMapping openMenuKey;
    private static boolean configLoaded;
    private static boolean brandedWindowTitle;

    public static String getVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    @Override
    public void onInitializeClient() {

        MODULE_MANAGER.register(new FpsModule());
        MODULE_MANAGER.register(new FpsBoosterModule());

        MODULE_MANAGER.register(new MemoryModule());
        MODULE_MANAGER.register(new CoordinatesModule());
        MODULE_MANAGER.register(new PingModule());
        MODULE_MANAGER.register(new SpeedModule());
        MODULE_MANAGER.register(new DirectionModule());
        MODULE_MANAGER.register(new ClockModule());
        MODULE_MANAGER.register(new SessionTimerModule());
        MODULE_MANAGER.register(new CpsModule());
        MODULE_MANAGER.register(new KeystrokesModule());
        MODULE_MANAGER.register(new PotCounterModule());
        MODULE_MANAGER.register(new SimpleModule("Watermark", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Target Health", ModuleCategory.COMBAT));
        MODULE_MANAGER.register(new SimpleModule("Item Counter", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Compass", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Saturation", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Frame Stats", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Biome", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Dimension", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Light Level", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("World Time", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Durability Status", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("XP Progress", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Last Death", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Inventory Space", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Offhand Counter", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Recent Gains", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Mount Health", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Flight Status", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Current Block", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Chunk Position", ModuleCategory.HUD));
        MODULE_MANAGER.register(new SimpleModule("Combo Counter", ModuleCategory.COMBAT));
        MODULE_MANAGER.register(new SimpleModule("Reach Display", ModuleCategory.COMBAT));

        MODULE_MANAGER.register(new ToggleSprintModule());
        MODULE_MANAGER.register(new ToggleSneakModule());
        MODULE_MANAGER.register(new ClearChatModule());
        MODULE_MANAGER.register(new AdvancedTooltipsModule());
        MODULE_MANAGER.register(new DurabilityWarningsModule());
        MODULE_MANAGER.register(new InventoryFullWarningsModule());
        MODULE_MANAGER.register(new LowHealthWarningModule());
        MODULE_MANAGER.register(new LowHungerWarningModule());
        MODULE_MANAGER.register(new LowAirWarningModule());

        MODULE_MANAGER.register(new FullbrightModule());
        MODULE_MANAGER.register(new NoDynamicFovModule());
        MODULE_MANAGER.register(new NoHurtCamModule());
        MODULE_MANAGER.register(new NoRainModule());
        MODULE_MANAGER.register(new SmoothCameraModule());
        MODULE_MANAGER.register(new ZoomModule());
        MODULE_MANAGER.register(new SimpleModule("Hide Scoreboard", ModuleCategory.VISUAL));
        MODULE_MANAGER.register(new SimpleModule("NoParticles", ModuleCategory.VISUAL));

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "maz_hud"),
                MazHudRenderDispatcher::render
        );

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "maz_client")
        );

        openMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.maz-client.open_menu",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        category
                )
        );

        ModuleHotkeys.registerAll(MODULE_MANAGER, category);

        // Minecraft owns every vanilla menu instance and lifecycle transition. MazClient
        // customizes those menus through render/init hooks instead of replacing TitleScreen
        // or PauseScreen objects while they are active. This preserves custom styling while
        // avoiding screen-instance swaps during startup, pause, saving, and disconnect.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!configLoaded) {
                configLoaded = true;
                ClientConfig.load(MODULE_MANAGER);
            }

            if (!brandedWindowTitle) {
                client.getWindow().setTitle("MazClient " + getVersion());
                brandedWindowTitle = true;
            }

            // Keep cached HUD telemetry on Minecraft's natural 20 Hz client cadence so the
            // uncapped render path can stay draw-only even on very high-FPS systems.
            MazHud.tick(client);
            SpecializedHudScheduler.tick(client);
            LastDeathHud.tick(client);

            while (openMenuKey.consumeClick()) {
                client.setScreen(new MazMenuScreen());
            }

            ParticleGroupMixin.maz$refreshParticleSettings();
            ModuleHotkeys.tick();
            MODULE_MANAGER.tick();
        });

        System.out.println("MazClient initialized!");
    }
}

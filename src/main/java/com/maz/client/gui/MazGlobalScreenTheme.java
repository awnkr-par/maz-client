package com.maz.client.gui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/**
 * Gives vanilla Minecraft menus MazClient colors without replacing screens or
 * changing Minecraft's widget layout.
 */
public final class MazGlobalScreenTheme implements ClientModInitializer {
    private static final int BG = 0xFF090E1A;
    private static final int BG_TOP = 0xFF11192A;
    private static final int PANEL_2 = 0xFF1C2942;
    private static final int BORDER = 0xFF2A3958;
    private static final int TEXT = 0xFFF8FAFC;
    private static final int MUTED = 0xFF94A3B8;
    private static final int ACCENT = 0xFF5865F2;
    private static final int DISABLED = 0xFF334155;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!shouldTheme(screen)) return;

            ScreenEvents.afterBackground(screen).register(MazGlobalScreenTheme::renderMazBackground);
            ScreenEvents.afterExtract(screen).register(MazGlobalScreenTheme::renderMazWidgets);
        });
    }

    private static boolean shouldTheme(Screen screen) {
        String className = screen.getClass().getName();

        // MazClient's module UI keeps its own custom layout and rendering.
        if (className.startsWith("com.maz.client.gui.")) return false;

        // Do not paint over gameplay-style screens.
        if (className.contains(".screens.inventory.")) return false;
        if (className.endsWith("ChatScreen") || className.endsWith("InBedChatScreen")) return false;
        if (className.endsWith("DeathScreen")) return false;
        if (className.endsWith("ReceivingLevelScreen") || className.endsWith("LevelLoadingScreen")) return false;
        if (className.endsWith("ProgressScreen")) return false;

        return className.startsWith("net.minecraft.client.gui.screens.");
    }

    private static void renderMazBackground(Screen screen, GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float tickProgress) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        graphics.fill(0, 0, width, height, BG);
        graphics.fill(0, 0, width, Math.max(110, height / 3), BG_TOP);
    }

    private static void renderMazWidgets(Screen screen, GuiGraphicsExtractor graphics,
                                         int mouseX, int mouseY, float tickProgress) {
        for (AbstractWidget widget : Screens.getWidgets(screen)) {
            if (!widget.visible) continue;

            if (widget instanceof AbstractButton) {
                if (isUtilityIconButton(widget)) {
                    renderUtilityIconFrame(graphics, widget, mouseX, mouseY);
                } else {
                    renderMazButton(graphics, widget, mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Small vanilla utility buttons (language, accessibility, mod/menu helpers, etc.)
     * often render an icon instead of meaningful button text. Do not paint over
     * their contents; preserve Minecraft's icon and add only Maz hover/border chrome.
     */
    private static boolean isUtilityIconButton(AbstractWidget widget) {
        return widget.getWidth() <= 40 && widget.getHeight() <= 40;
    }

    private static void renderUtilityIconFrame(GuiGraphicsExtractor graphics, AbstractWidget widget,
                                               int mouseX, int mouseY) {
        int left = widget.getX() - 1;
        int top = widget.getY() - 1;
        int right = widget.getX() + widget.getWidth() + 1;
        int bottom = widget.getY() + widget.getHeight() + 1;

        int border = widget.active && widget.isMouseOver(mouseX, mouseY) ? ACCENT : BORDER;

        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
    }

    private static void renderMazButton(GuiGraphicsExtractor graphics, AbstractWidget widget,
                                        int mouseX, int mouseY) {
        int left = widget.getX();
        int top = widget.getY();
        int right = left + widget.getWidth();
        int bottom = top + widget.getHeight();

        boolean hovered = widget.active && widget.isMouseOver(mouseX, mouseY);
        int fill = widget.active ? (hovered ? ACCENT : PANEL_2) : DISABLED;
        int border = hovered ? ACCENT : BORDER;
        int text = widget.active ? TEXT : MUTED;

        graphics.fill(left, top, right, bottom, fill);
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);

        Minecraft client = Minecraft.getInstance();
        graphics.centeredText(
                client.font,
                widget.getMessage().getString(),
                (left + right) / 2,
                top + Math.max(1, (widget.getHeight() - 8) / 2),
                text
        );
    }
}

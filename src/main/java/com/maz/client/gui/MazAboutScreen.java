package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MazAboutScreen extends Screen {

    private static final int BG = 0xFF090E1A;
    private static final int BG_TOP = 0xFF11192A;
    private static final int PANEL = 0xFF141E31;
    private static final int PANEL_2 = 0xFF1C2942;
    private static final int BORDER = 0xFF2A3958;
    private static final int TEXT = 0xFFF8FAFC;
    private static final int MUTED = 0xFF94A3B8;
    private static final int ACCENT = 0xFF5865F2;
    private static final int ACCENT_HOVER = 0xFF6875FF;
    private static final int SUCCESS = 0xFF22C55E;

    private static final int WIDTH = 500;
    private static final int HEIGHT = 310;

    private final Screen parent;

    public MazAboutScreen(Screen parent) {
        super(Component.literal("About MazClient"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, BG);
        graphics.fill(0, 0, this.width, Math.max(110, this.height / 3), BG_TOP);

        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        int right = left + WIDTH;
        int bottom = top + HEIGHT;

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, PANEL);

        graphics.fill(left + 24, top + 24, left + 72, top + 72, ACCENT);
        graphics.centeredText(this.font, "M", left + 48, top + 42, 0xFFFFFFFF);
        graphics.text(this.font, "MazClient", left + 88, top + 28, TEXT, false);
        graphics.text(this.font, "Client information", left + 88, top + 48, MUTED, false);

        int total = MazClient.MODULE_MANAGER.getModules().size();
        int enabled = 0;
        for (Module module : MazClient.MODULE_MANAGER.getModules()) {
            if (module.isEnabled()) enabled++;
        }

        int infoTop = top + 94;
        drawInfoRow(graphics, left + 24, right - 24, infoTop, "Version", MazClient.getVersion(), false);
        drawInfoRow(graphics, left + 24, right - 24, infoTop + 34, "Modules", enabled + " active / " + total + " installed", enabled > 0);
        drawInfoRow(graphics, left + 24, right - 24, infoTop + 68, "Module menu", "Right Shift", false);
        drawInfoRow(graphics, left + 24, right - 24, infoTop + 102, "Platform", "Fabric • Minecraft 1.21.11", false);

        graphics.text(this.font, "MazClient keeps your HUD and client controls together in one lightweight menu.",
                left + 24, top + 242, MUTED, false);

        int backLeft = left + 24;
        int backRight = right - 24;
        int backTop = bottom - 44;
        boolean hovered = inside(mouseX, mouseY, backLeft, backTop, backRight, bottom - 16);
        graphics.fill(backLeft, backTop, backRight, bottom - 16, hovered ? ACCENT_HOVER : ACCENT);
        graphics.centeredText(this.font, "Back", (backLeft + backRight) / 2, backTop + 10, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawInfoRow(GuiGraphicsExtractor graphics, int left, int right, int top,
                             String label, String value, boolean success) {
        graphics.fill(left, top, right, top + 28, PANEL_2);
        graphics.fill(left, top, left + 3, top + 28, success ? SUCCESS : BORDER);
        graphics.text(this.font, label, left + 10, top + 10, MUTED, false);
        graphics.text(this.font, value, right - 180, top + 10, success ? SUCCESS : TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        int right = left + WIDTH;
        int bottom = top + HEIGHT;

        if (inside(event.x(), event.y(), left + 24, bottom - 44, right - 24, bottom - 16)) {
            Minecraft.getInstance().gui.setScreen(parent);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().gui.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean inside(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

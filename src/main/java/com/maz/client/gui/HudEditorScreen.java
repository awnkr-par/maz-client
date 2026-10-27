package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.Module;
import com.maz.client.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {

    private static final int BG = 0xFF090E1A;
    private static final int BG_TOP = 0xFF11192A;
    private static final int PANEL = 0xFF141E31;
    private static final int PANEL_2 = 0xFF1C2942;
    private static final int BORDER = 0xFF2A3958;
    private static final int GRID = 0xFF1C2942;
    private static final int TEXT = 0xFFF8FAFC;
    private static final int MUTED = 0xFF94A3B8;
    private static final int ACCENT = 0xFF5865F2;
    private static final int ACCENT_HOVER = 0xFF6875FF;
    private static final int SELECT = 0x665865F2;

    private String draggingModule;
    private String selectedModule;
    private boolean opacityDragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditorScreen() {
        super(Component.literal("MazClient HUD Editor"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, BG);
        graphics.fill(0, 0, this.width, 42, BG_TOP);

        for (int x = 0; x < this.width; x += 20) graphics.fill(x, 42, x + 1, this.height - 38, GRID);
        for (int y = 42; y < this.height - 38; y += 20) graphics.fill(0, y, this.width, y + 1, GRID);

        Minecraft client = Minecraft.getInstance();
        int defaultY = 48;

        for (Module module : MazClient.MODULE_MANAGER.getModules()) {
            if (!isDraggableHudModule(module)) continue;

            HudLayout.Position p = HudLayout.getPosition(module.getName(), 8, defaultY);
            int w = MazHud.previewWidth(client, module.getName());
            int h = MazHud.previewHeight(module.getName());

            if (module.getName().equals(selectedModule) || module.getName().equals(draggingModule)) {
                graphics.fill(p.x() - 2, p.y() - 2, p.x() + w + 2, p.y() + h + 2, SELECT);
            } else if (mouseX >= p.x() && mouseX <= p.x() + w && mouseY >= p.y() && mouseY <= p.y() + h) {
                graphics.fill(p.x() - 1, p.y() - 1, p.x() + w + 1, p.y() + h + 1, SELECT);
            }

            MazHud.drawPreview(graphics, client, module.getName(), p.x(), p.y());
            defaultY += 22;
        }

        graphics.fill(0, 41, this.width, 42, BORDER);
        graphics.fill(10, 8, 36, 34, ACCENT);
        graphics.centeredText(this.font, "M", 23, 17, 0xFFFFFFFF);
        graphics.text(this.font, "MazClient HUD Editor", 46, 9, TEXT, false);
        graphics.text(this.font, "Drag modules • click one to edit opacity • ESC saves", 46, 24, MUTED, false);

        int resetLeft = this.width - 80;
        boolean resetHover = mouseX >= resetLeft && mouseX <= this.width - 10 && mouseY >= 8 && mouseY <= 32;
        graphics.fill(resetLeft, 8, this.width - 10, 32, resetHover ? ACCENT_HOVER : ACCENT);
        graphics.centeredText(this.font, "Reset", resetLeft + 35, 16, 0xFFFFFFFF);

        drawOpacityControl(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawOpacityControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int panelTop = this.height - 38;
        graphics.fill(0, panelTop, this.width, this.height, PANEL);
        graphics.fill(0, panelTop, this.width, panelTop + 1, BORDER);

        if (selectedModule == null) {
            graphics.centeredText(this.font, "Select a HUD element to change opacity or position", this.width / 2, panelTop + 15, MUTED);
            return;
        }

        int alpha = HudLayout.getOpacity(selectedModule);
        int percent = Math.round(alpha * 100.0F / 255.0F);
        int sliderLeft = 150;
        int sliderRight = Math.max(sliderLeft + 60, this.width - 160);
        int sliderY = panelTop + 18;

        graphics.text(this.font, selectedModule + "  " + percent + "%", 12, panelTop + 14, TEXT, false);
        graphics.fill(sliderLeft, sliderY, sliderRight, sliderY + 4, PANEL_2);

        int knobX = sliderLeft + Math.round((alpha / 255.0F) * (sliderRight - sliderLeft));
        graphics.fill(sliderLeft, sliderY, knobX, sliderY + 4, ACCENT);
        graphics.fill(knobX - 3, sliderY - 4, knobX + 3, sliderY + 8, TEXT);

        int centerLeft = this.width - 145;
        int opacityResetLeft = this.width - 75;
        boolean centerHover = mouseX >= centerLeft && mouseX <= centerLeft + 60 && mouseY >= panelTop + 8 && mouseY <= panelTop + 30;
        boolean opacityHover = mouseX >= opacityResetLeft && mouseX <= opacityResetLeft + 60 && mouseY >= panelTop + 8 && mouseY <= panelTop + 30;
        graphics.fill(centerLeft, panelTop + 8, centerLeft + 60, panelTop + 30, centerHover ? ACCENT : PANEL_2);
        graphics.fill(centerLeft, panelTop + 8, centerLeft + 60, panelTop + 9, centerHover ? ACCENT : BORDER);
        graphics.centeredText(this.font, "Center", centerLeft + 30, panelTop + 15, centerHover ? 0xFFFFFFFF : TEXT);
        graphics.fill(opacityResetLeft, panelTop + 8, opacityResetLeft + 60, panelTop + 30, opacityHover ? ACCENT : PANEL_2);
        graphics.fill(opacityResetLeft, panelTop + 8, opacityResetLeft + 60, panelTop + 9, opacityHover ? ACCENT : BORDER);
        graphics.centeredText(this.font, "100%", opacityResetLeft + 30, panelTop + 15, opacityHover ? 0xFFFFFFFF : TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int resetLeft = this.width - 80;
        if (event.x() >= resetLeft && event.x() <= this.width - 10 && event.y() >= 8 && event.y() <= 32) {
            HudLayout.reset();
            return true;
        }

        if (selectedModule != null && event.y() >= this.height - 38) {
            int panelTop = this.height - 38;
            int centerLeft = this.width - 145;
            int opacityResetLeft = this.width - 75;

            if (event.x() >= centerLeft && event.x() <= centerLeft + 60 && event.y() >= panelTop + 8 && event.y() <= panelTop + 30) {
                centerSelectedModule();
                HudLayout.save();
                return true;
            }

            if (event.x() >= opacityResetLeft && event.x() <= opacityResetLeft + 60 && event.y() >= panelTop + 8 && event.y() <= panelTop + 30) {
                HudLayout.setOpacity(selectedModule, 255);
                HudLayout.save();
                return true;
            }

            opacityDragging = true;
            updateOpacityFromMouse(event.x());
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        int defaultY = 48;

        for (Module module : MazClient.MODULE_MANAGER.getModules()) {
            if (!isDraggableHudModule(module)) continue;

            HudLayout.Position p = HudLayout.getPosition(module.getName(), 8, defaultY);
            int w = MazHud.previewWidth(client, module.getName());
            int h = MazHud.previewHeight(module.getName());

            if (event.x() >= p.x() && event.x() <= p.x() + w && event.y() >= p.y() && event.y() <= p.y() + h) {
                selectedModule = module.getName();
                draggingModule = module.getName();
                dragOffsetX = (int) event.x() - p.x();
                dragOffsetY = (int) event.y() - p.y();
                return true;
            }

            defaultY += 22;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void centerSelectedModule() {
        if (selectedModule == null) return;
        Minecraft client = Minecraft.getInstance();
        int w = MazHud.previewWidth(client, selectedModule);
        int h = MazHud.previewHeight(selectedModule);
        int availableHeight = Math.max(0, this.height - 80);
        int x = Math.max(0, (this.width - w) / 2);
        int y = Math.max(42, 42 + (availableHeight - h) / 2);
        HudLayout.setPosition(selectedModule, x, y);
    }

    private static boolean isDraggableHudModule(Module module) {
        boolean hudElement = module.getCategory() == ModuleCategory.HUD
                || module.getCategory() == ModuleCategory.COMBAT
                || module.getName().equalsIgnoreCase("FPS");
        return hudElement && module.isEnabled() && !module.getName().equalsIgnoreCase("Saturation");
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && opacityDragging && selectedModule != null) {
            updateOpacityFromMouse(event.x());
            return true;
        }

        if (event.button() == 0 && draggingModule != null) {
            Minecraft client = Minecraft.getInstance();
            int w = MazHud.previewWidth(client, draggingModule);
            int h = MazHud.previewHeight(draggingModule);
            int x = (int) event.x() - dragOffsetX;
            int y = (int) event.y() - dragOffsetY;
            x = Math.max(0, Math.min(this.width - w, x));
            y = Math.max(42, Math.min(this.height - 42 - h, y));
            HudLayout.setPosition(draggingModule, x, y);
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    private void updateOpacityFromMouse(double mouseX) {
        if (selectedModule == null) return;
        int sliderLeft = 150;
        int sliderRight = Math.max(sliderLeft + 60, this.width - 160);
        double t = (mouseX - sliderLeft) / (double) (sliderRight - sliderLeft);
        t = Math.max(0.0, Math.min(1.0, t));
        HudLayout.setOpacity(selectedModule, (int) Math.round(t * 255.0));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            boolean handled = opacityDragging || draggingModule != null;
            opacityDragging = false;
            draggingModule = null;
            if (handled) {
                HudLayout.save();
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override public void onClose() { HudLayout.save(); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}

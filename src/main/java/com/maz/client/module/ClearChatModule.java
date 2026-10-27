package com.maz.client.module;

import net.minecraft.client.Minecraft;

public class ClearChatModule extends Module {

    public ClearChatModule() {
        super("Clear Chat", ModuleCategory.UTILITY);
    }

    @Override
    public boolean isAction() {
        return true;
    }

    @Override
    public void toggle() {
        runAction();
    }

    @Override
    public void runAction() {
        Minecraft client = Minecraft.getInstance();
        client.gui.hud.getChat().clearMessages(true);
    }
}

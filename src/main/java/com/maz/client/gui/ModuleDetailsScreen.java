package com.maz.client.gui;

import com.maz.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ModuleDetailsScreen extends Screen {
    private static final int BG=0xFF090E1A,BG_TOP=0xFF11192A,PANEL=0xFF141E31,PANEL2=0xFF1C2942,BORDER=0xFF2A3958,TEXT=0xFFF8FAFC,MUTED=0xFF94A3B8,ACCENT=0xFF5865F2,SUCCESS=0xFF22C55E,DANGER=0xFFEF4444;
    private static final int W=500,H=250;
    private final Screen parent;
    private final Module module;

    public ModuleDetailsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName()));
        this.parent = parent;
        this.module = module;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        g.fill(0,0,width,height,BG); g.fill(0,0,width,Math.max(110,height/3),BG_TOP);
        int l=(width-W)/2,t=(height-H)/2,r=l+W,b=t+H;
        g.fill(l-1,t-1,r+1,b+1,BORDER); g.fill(l,t,r,b,PANEL);
        g.fill(l+22,t+20,l+62,t+60,ACCENT); g.centeredText(font,"M",l+42,t+35,0xFFFFFFFF);
        g.text(font,module.getName(),l+76,t+24,TEXT,false);
        g.text(font,module.getCategory().getDisplayName()+" module",l+76,t+43,MUTED,false);

        g.fill(l+22,t+82,r-22,t+140,PANEL2);
        g.fill(l+22,t+82,l+25,t+140,ACCENT);
        g.text(font,"Description",l+34,t+94,MUTED,false);
        g.text(font,trim(module.getDescription(),72),l+34,t+116,TEXT,false);

        if(module.isAction()){
            g.fill(l+22,t+158,l+172,t+188,ACCENT); g.centeredText(font,"RUN ACTION",l+97,t+169,0xFFFFFFFF);
        } else {
            g.text(font,"Enabled",l+22,t+168,TEXT,false);
            int tl=r-92,tr=r-32,tt=t+158,tb=t+186;
            g.fill(tl,tt,tr,tb,module.isEnabled()?SUCCESS:BORDER);
            int knob=module.isEnabled()?tr-24:tl+4;
            g.fill(knob,tt+4,knob+20,tb-4,0xFFF8FAFC);
        }

        if(hasSettings()){
            boolean hover=inside(mx,my,l+188,t+158,r-22,t+188);
            g.fill(l+188,t+158,r-22,t+188,hover?ACCENT:PANEL2);
            g.centeredText(font,"OPEN SETTINGS",(l+188+r-22)/2,t+169,hover?0xFFFFFFFF:TEXT);
        }
        boolean backHover=inside(mx,my,l+22,b-42,r-22,b-14);
        g.fill(l+22,b-42,r-22,b-14,backHover?ACCENT:PANEL2); g.centeredText(font,"Back",(l+r)/2,b-32,backHover?0xFFFFFFFF:TEXT);
        super.extractRenderState(g,mx,my,d);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e,boolean dc){
        if(e.button()!=0) return super.mouseClicked(e,dc);
        int l=(width-W)/2,t=(height-H)/2,r=l+W,b=t+H; double x=e.x(),y=e.y();
        if(module.isAction()){
            if(inside(x,y,l+22,t+158,l+172,t+188)){ module.runAction(); return true; }
        } else {
            if(inside(x,y,r-92,t+158,r-32,t+186)){ module.toggle(); return true; }
        }
        if(hasSettings() && inside(x,y,l+188,t+158,r-22,t+188)){
            Minecraft.getInstance().gui.setScreen(new FpsBoosterScreen(this)); return true;
        }
        if(inside(x,y,l+22,b-42,r-22,b-14)){ Minecraft.getInstance().gui.setScreen(parent); return true; }
        return super.mouseClicked(e,dc);
    }

    private boolean hasSettings(){ return module.getName().equalsIgnoreCase("FPS Booster"); }
    private static String trim(String s,int max){ if(s==null)return ""; return s.length()<=max?s:s.substring(0,max-3)+"..."; }
    private static boolean inside(double x,double y,int l,int t,int r,int b){ return x>=l&&x<=r&&y>=t&&y<=b; }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e){ if(e.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){ Minecraft.getInstance().gui.setScreen(parent); return true; } return super.keyPressed(e); }
    @Override public boolean isPauseScreen(){ return false; }
}

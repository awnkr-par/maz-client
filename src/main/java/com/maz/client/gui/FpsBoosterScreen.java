package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.FpsBoosterModule;
import com.maz.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class FpsBoosterScreen extends Screen {
    private static final int BG=0xFF090E1A,BG_TOP=0xFF11192A,PANEL=0xFF141E31,PANEL2=0xFF1C2942,BORDER=0xFF2A3958,TEXT=0xFFF8FAFC,MUTED=0xFF94A3B8,ACCENT=0xFF5865F2,SUCCESS=0xFF22C55E,DANGER=0xFFEF4444;
    private static final int W=560,H=382;
    private final Screen parent;
    private final FpsBoosterModule booster;

    public FpsBoosterScreen(Screen parent){
        super(Component.literal("FPS Booster"));
        this.parent=parent;
        Module m=MazClient.MODULE_MANAGER.getModule("FPS Booster");
        this.booster=m instanceof FpsBoosterModule f?f:null;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        g.fill(0,0,width,height,BG); g.fill(0,0,width,Math.max(110,height/3),BG_TOP); int l=(width-W)/2,t=(height-H)/2,r=l+W,b=t+H;
        g.fill(l-1,t-1,r+1,b+1,BORDER); g.fill(l,t,r,b,PANEL);
        g.fill(l+22,t+20,l+64,t+62,ACCENT); g.drawCenteredString(font,"FPS",l+43,t+36,0xFFFFFFFF);
        g.drawString(font,"FPS Booster Tuning",l+78,t+24,TEXT,false);
        g.drawString(font,"Direct controls only. Render and simulation distance stay player-controlled.",l+78,t+42,MUTED,false);
        boolean en=booster!=null&&booster.isEnabled(); g.fill(r-130,t+24,r-22,t+52,en?SUCCESS:DANGER); g.drawCenteredString(font,en?"ENABLED":"DISABLED",r-76,t+34,0xFFFFFFFF);

        g.drawString(font,"Minecraft Settings",l+22,t+82,TEXT,false);
        readonly(g,l+22,r-22,t+98,"Render Distance",Minecraft.getInstance().options.renderDistance().get()+" chunks");
        readonly(g,l+22,r-22,t+136,"Simulation Distance",Minecraft.getInstance().options.simulationDistance().get()+" chunks");

        g.drawString(font,"FPS Booster  |  Cap: "+Minecraft.getInstance().options.framerateLimit().get()+"  |  VSync: "+(Minecraft.getInstance().options.enableVsync().get()?"ON":"OFF"),l+22,t+182,TEXT,false);
        value(g,l+22,r-22,t+198,"Entity Distance",booster==null?"-":booster.getEntityDistancePercent()+"%");
        value(g,l+22,r-22,t+236,"Particle Density",booster==null?"-":particleLabel(booster.getParticleKeepEvery()));
        toggle(g,l+22,r-22,t+274,"Entity Shadows",booster!=null&&booster.getEntityShadows());
        boolean maxHover=inside(mx,my,l+22,t+310,r-22,t+338);
        g.fill(l+22,t+310,r-22,t+338,maxHover?0xFF6875FF:ACCENT);
        g.drawCenteredString(font,"MAX FPS (NO CAP CHANGE)",(l+r)/2,t+320,0xFFFFFFFF);

        boolean backHover=inside(mx,my,l+22,b-42,r-22,b-14); g.fill(l+22,b-42,r-22,b-14,backHover?0xFF6875FF:ACCENT); g.drawCenteredString(font,"Back",(l+r)/2,b-32,0xFFFFFFFF); super.extractRenderState(g,mx,my,d);
    }

    private void readonly(GuiGraphicsExtractor g,int l,int r,int y,String label,String val){
        g.fill(l,y,r,y+30,PANEL2);g.fill(l,y,l+3,y+30,BORDER);g.drawString(font,label,l+10,y+11,TEXT,false);g.drawString(font,"Minecraft controlled",r-270,y+11,MUTED,false);g.drawString(font,val,r-130,y+11,MUTED,false);
    }
    private void value(GuiGraphicsExtractor g,int l,int r,int y,String label,String val){g.fill(l,y,r,y+30,PANEL2);g.fill(l,y,l+3,y+30,ACCENT);g.drawString(font,label,l+10,y+11,TEXT,false);g.drawString(font,val,r-190,y+11,MUTED,false);g.fill(r-66,y+4,r-38,y+26,BORDER);g.drawCenteredString(font,"-",r-52,y+10,TEXT);g.fill(r-32,y+4,r-4,y+26,BORDER);g.drawCenteredString(font,"+",r-18,y+10,TEXT);}
    private void toggle(GuiGraphicsExtractor g,int l,int r,int y,String label,boolean en){g.fill(l,y,r,y+30,PANEL2);g.fill(l,y,l+3,y+30,ACCENT);g.drawString(font,label,l+10,y+11,TEXT,false);g.fill(r-74,y+5,r-4,y+25,en?SUCCESS:DANGER);g.drawCenteredString(font,en?"ON":"OFF",r-39,y+11,0xFFFFFFFF);}

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean dc){if(e.button()!=0||booster==null)return super.mouseClicked(e,dc);int l=(width-W)/2,t=(height-H)/2,r=l+W,b=t+H;double x=e.x(),y=e.y();
        if(inside(x,y,r-130,t+24,r-22,t+52)){booster.setEnabled(!booster.isEnabled());return true;}
        if(adjust(x,y,t+198,r,()->booster.setEntityDistancePercent(booster.getEntityDistancePercent()-10),()->booster.setEntityDistancePercent(booster.getEntityDistancePercent()+10)))return true;
        if(adjust(x,y,t+236,r,()->booster.setParticleKeepEvery(booster.getParticleKeepEvery()+1),()->booster.setParticleKeepEvery(booster.getParticleKeepEvery()-1)))return true;
        if(inside(x,y,r-74,t+279,r-4,t+299)){booster.setEntityShadows(!booster.getEntityShadows());return true;}
        if(inside(x,y,l+22,t+310,r-22,t+338)){booster.applyMaxFpsProfile();return true;}
        if(inside(x,y,l+22,b-42,r-22,b-14)){Minecraft.getInstance().gui.setScreen(parent);return true;} return super.mouseClicked(e,dc);
    }
    private boolean adjust(double x,double y,int row,int r,Runnable minus,Runnable plus){if(inside(x,y,r-66,row+4,r-38,row+26)){minus.run();return true;}if(inside(x,y,r-32,row+4,r-4,row+26)){plus.run();return true;}return false;}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e){if(e.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){Minecraft.getInstance().gui.setScreen(parent);return true;}return super.keyPressed(e);}
    private static String particleLabel(int k){return k<=1?"100%":"~"+Math.max(1,100/k)+"%";}
    private static boolean inside(double x,double y,int l,int t,int r,int b){return x>=l&&x<=r&&y>=t&&y<=b;}
    @Override public boolean isPauseScreen(){return false;}
}

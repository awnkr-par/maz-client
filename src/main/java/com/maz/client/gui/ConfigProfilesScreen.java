package com.maz.client.gui;

import com.maz.client.config.ConfigBundle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ConfigProfilesScreen extends Screen {
    private static final int BG=0xFF090E1A,PANEL=0xFF141E31,PANEL2=0xFF1C2942,BORDER=0xFF2A3958,TEXT=0xFFF8FAFC,MUTED=0xFF94A3B8,ACCENT=0xFF5865F2,SUCCESS=0xFF22C55E,DANGER=0xFFEF4444;
    private static final int W=520,H=300;
    private final Screen parent;
    private String status="";

    public ConfigProfilesScreen(Screen parent){
        super(Component.literal("Quick Config Profiles"));
        this.parent=parent;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        g.fill(0,0,width,height,BG);
        int l=(width-W)/2,t=(height-H)/2,r=l+W,b=t+H;
        g.fill(l-1,t-1,r+1,b+1,BORDER); g.fill(l,t,r,b,PANEL);
        g.fill(l+16,t+16,l+52,t+52,ACCENT); g.text(font,"M",l+30,t+30,0xFFFFFFFF,true);
        g.text(font,"Quick Config Profiles",l+64,t+20,TEXT,false);
        g.text(font,"Minecraft options + Maz modules + HUD layout.",l+64,t+37,MUTED,false);
        g.fill(l,t+66,r,t+67,BORDER);

        int y=t+82;
        for(int slot=1;slot<=3;slot++){
            boolean exists=ConfigBundle.profileExists(slot);
            g.fill(l+22,y,r-22,y+48,PANEL2);
            g.text(font,"Profile "+slot,l+34,y+10,TEXT,false);
            g.text(font,exists?"SAVED":"EMPTY",l+34,y+28,exists?SUCCESS:MUTED,false);
            button(g,mx,my,r-226,y+11,r-166,y+37,"SAVE",ACCENT);
            button(g,mx,my,r-158,y+11,r-98,y+37,"LOAD",exists?SUCCESS:BORDER);
            button(g,mx,my,r-90,y+11,r-30,y+37,"DELETE",exists?DANGER:BORDER);
            y+=58;
        }

        if(!status.isEmpty()) g.text(font,trim(status,66),l+22,b-48,MUTED,false);
        button(g,mx,my,l+22,b-36,r-22,b-12,"BACK",ACCENT);
        super.extractRenderState(g,mx,my,d);
    }

    private void button(GuiGraphicsExtractor g,int mx,int my,int l,int t,int r,int b,String label,int color){
        boolean hover=inside(mx,my,l,t,r,b);
        g.fill(l,t,r,b,hover?0xFF6875FF:color);
        g.centeredText(font,label,(l+r)/2,t+9,0xFFFFFFFF);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean dc){
        if(e.button()!=0)return super.mouseClicked(e,dc);
        int l=(width-W)/2,t=(height-H)/2,r=l+W,b=t+H;
        int y=t+82;
        for(int slot=1;slot<=3;slot++){
            final int selected=slot;
            if(inside(e.x(),e.y(),r-226,y+11,r-166,y+37)){run("Saved Profile "+slot,()->ConfigBundle.saveProfile(selected));return true;}
            if(inside(e.x(),e.y(),r-158,y+11,r-98,y+37)){
                if(!ConfigBundle.profileExists(slot)){status="Profile "+slot+" is empty.";return true;}
                run("Loaded Profile "+slot+" • restart for Minecraft settings",()->ConfigBundle.loadProfile(selected));return true;
            }
            if(inside(e.x(),e.y(),r-90,y+11,r-30,y+37)){
                if(!ConfigBundle.profileExists(slot)){status="Profile "+slot+" is already empty.";return true;}
                run("Deleted Profile "+slot,()->ConfigBundle.deleteProfile(selected));return true;
            }
            y+=58;
        }
        if(inside(e.x(),e.y(),l+22,b-36,r-22,b-12)){Minecraft.getInstance().gui.setScreen(parent);return true;}
        return super.mouseClicked(e,dc);
    }

    private void run(String success,IoAction action){
        try{action.run();status=success;}catch(Exception ex){status="Failed: "+ex.getMessage();}
    }

    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e){
        if(e.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){Minecraft.getInstance().gui.setScreen(parent);return true;}
        return super.keyPressed(e);
    }

    private static String trim(String s,int max){return s.length()<=max?s:s.substring(0,max-3)+"...";}
    private static boolean inside(double x,double y,int l,int t,int r,int b){return x>=l&&x<=r&&y>=t&&y<=b;}
    @Override public boolean isPauseScreen(){return false;}

    @FunctionalInterface private interface IoAction{void run() throws Exception;}
}

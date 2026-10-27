package com.maz.client.gui;

import com.maz.client.MazClient;
import com.maz.client.module.CombatStats;
import com.maz.client.module.CpsModule;
import com.maz.client.module.Module;
import com.maz.client.module.LivePingProbe;
import com.maz.client.module.PingModule;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MazHud {
    private static final int BACKGROUND = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5865F2;
    private static final int MEMORY_OK = 0xFF43A047;
    private static final int MEMORY_WARN = 0xFFFFB300;
    private static final int MEMORY_HIGH = 0xFFE53935;
    private static final int PRESSED_TEXT = 0xFFFFFFFF;
    private static final int KEY_BG = 0xFFFFFFFF;
    private static final int SATURATION_FULL = 0xFFFFC107;
    private static final int SATURATION_HALF = 0xFFFFD54F;
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<String, HudWidthCacheEntry> HUD_WIDTH_CACHE = new HashMap<>();

    private static final HudLayout.Binding FPS_LAYOUT = HudLayout.bind("FPS", 8, 8);
    private static final HudLayout.Binding MEMORY_LAYOUT = HudLayout.bind("Memory", 8, 30);
    private static final HudLayout.Binding COORDINATES_LAYOUT = HudLayout.bind("Coordinates", 8, 52);
    private static final HudLayout.Binding PING_LAYOUT = HudLayout.bind("Ping", 8, 74);
    private static final HudLayout.Binding SPEED_LAYOUT = HudLayout.bind("Speed", 8, 96);
    private static final HudLayout.Binding DIRECTION_LAYOUT = HudLayout.bind("Direction", 8, 118);
    private static final HudLayout.Binding CLOCK_LAYOUT = HudLayout.bind("Clock", 8, 140);
    private static final HudLayout.Binding SESSION_LAYOUT = HudLayout.bind("Session Timer", 8, 162);
    private static final HudLayout.Binding CPS_LAYOUT = HudLayout.bind("CPS", 8, 184);
    private static final HudLayout.Binding KEYSTROKES_LAYOUT = HudLayout.bind("Keystrokes", 8, 206);
    private static final HudLayout.Binding POT_COUNTER_LAYOUT = HudLayout.bind("PotCounter", 8, 272);
    private static final HudLayout.Binding WATERMARK_LAYOUT = HudLayout.bind("Watermark", 8, 294);
    private static final HudLayout.Binding TARGET_HEALTH_LAYOUT = HudLayout.bind("Target Health", 8, 316);
    private static final HudLayout.Binding ITEM_COUNTER_LAYOUT = HudLayout.bind("Item Counter", 8, 338);
    private static final HudLayout.Binding ARMOR_DURABILITY_LAYOUT = HudLayout.bind("Armor Durability", 8, 360);
    private static final HudLayout.Binding COMPASS_LAYOUT = HudLayout.bind("Compass", 8, 382);
    private static final HudLayout.Binding ARMOR_LAYOUT = HudLayout.bind("Armor HUD", 8, 404);
    private static final HudLayout.Binding COMBO_LAYOUT = HudLayout.bind("Combo Counter", 8, 426);
    private static final HudLayout.Binding REACH_LAYOUT = HudLayout.bind("Reach Display", 8, 448);
    private static final HudLayout.Binding POTION_LAYOUT = HudLayout.bind("Potion HUD", 8, 470);

    private static boolean modulesResolved;
    private static Module saturationModule, fpsModule, memoryModule, coordinatesModule, pingModule, speedModule,
            directionModule, clockModule, sessionModule, cpsModule, keystrokesModule, potCounterModule,
            watermarkModule, targetHealthModule, itemCounterModule, armorDurabilityModule, compassModule,
            armorModule, comboModule, reachModule, potionHudModule;

    private static long lastSlowRefreshMs;
    private static int telemetryPhase;
    private static boolean hadPlayer;
    private static String fpsText = "FPS: --";
    private static String pingText = "Ping: -- ms";
    private static String cpsText = "CPS: L 0 | R 0";
    private static String potCounterText = "Pots: 0";
    private static String memoryText = "RAM: --";
    private static int memoryAccent = MEMORY_OK;
    private static String coordinatesText = "XYZ: --";
    private static String speedText = "Speed: -- b/s";
    private static String directionText = "Facing: --";
    private static String clockText = "Time: --";
    private static String sessionText = "Session: --";
    private static String itemCounterText = "Item: Empty";
    private static String armorDurabilityText = "Armor: none";
    private static String compassText = "--";
    private static String armorHudText = "Helmet Empty | Chest Empty | Legs Empty | Boots Empty";
    private static String potionText = "Effects: none";
    private static String targetHealthText = "";
    private static String comboText = "Combo: 0";
    private static String reachText = "Reach: 0.00";
    private static boolean hasTargetHealth;

    private static Font cachedKeyWidthFont;
    private static int keyWidthW, keyWidthA, keyWidthS, keyWidthD, keyWidthLmb, keyWidthRmb;

    public static void tick(Minecraft client) {
        resolveModules();
        refreshRealtimeText(client);
        if (enabled(pingModule)) LivePingProbe.tick(client);

        if (client.player == null) {
            if (hadPlayer) resetPlayerTelemetry();
            hadPlayer = false;
            telemetryPhase = 0;
        } else {
            // Ping is intentionally checked every client tick so the HUD reflects a new
            // server-reported latency value as soon as Minecraft exposes it. This does
            // not send extra packets; it only reads the latest PlayerInfo value.
            refreshPingText(client);

            if (!hadPlayer) {
                refreshInventoryText(client);
                refreshEquipmentText(client);
                refreshPotionText(client);
                hadPlayer = true;
                telemetryPhase = 0;
            } else {
                // Keep telemetry fresh, but skip all inventory/equipment/effect work
                // when its corresponding HUD modules are disabled.
                switch (telemetryPhase) {
                    case 0 -> {
                        if (enabled(potCounterModule) || enabled(itemCounterModule)) refreshInventoryText(client);
                    }
                    case 1 -> {
                        if (enabled(armorDurabilityModule) || enabled(armorModule)) refreshEquipmentText(client);
                    }
                    case 2 -> {
                        if (enabled(potionHudModule)) refreshPotionText(client);
                    }
                    default -> { }
                }
                telemetryPhase = (telemetryPhase + 1) % 4;
            }
        }

        long now = System.currentTimeMillis();
        if (lastSlowRefreshMs == 0L || now - lastSlowRefreshMs >= 500L) {
            lastSlowRefreshMs = now;
            refreshSlowText(client, now);
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        resolveModules();

        if (enabled(saturationModule) && client.player != null) drawSaturationOnHungerBar(graphics, client);
        if (enabled(fpsModule)) drawHudBox(graphics, client, "FPS", fpsText, FPS_LAYOUT);
        if (enabled(memoryModule)) drawHudBox(graphics, client, "Memory", memoryText, MEMORY_LAYOUT, memoryAccent);
        if (enabled(coordinatesModule) && client.player != null) drawHudBox(graphics, client, "Coordinates", coordinatesText, COORDINATES_LAYOUT);
        if (enabled(pingModule) && client.player != null && client.getConnection() != null) drawHudBox(graphics, client, "Ping", pingText, PING_LAYOUT);
        if (enabled(speedModule) && client.player != null) drawHudBox(graphics, client, "Speed", speedText, SPEED_LAYOUT);
        if (enabled(directionModule) && client.player != null) drawHudBox(graphics, client, "Direction", directionText, DIRECTION_LAYOUT);
        if (enabled(clockModule)) drawHudBox(graphics, client, "Clock", clockText, CLOCK_LAYOUT);
        if (enabled(sessionModule)) drawHudBox(graphics, client, "Session Timer", sessionText, SESSION_LAYOUT);
        if (enabled(cpsModule)) drawHudBox(graphics, client, "CPS", cpsText, CPS_LAYOUT);
        if (enabled(keystrokesModule)) drawKeystrokes(graphics, client, KEYSTROKES_LAYOUT);
        if (enabled(potCounterModule)) drawHudBox(graphics, client, "PotCounter", potCounterText, POT_COUNTER_LAYOUT);
        if (enabled(watermarkModule)) drawHudBox(graphics, client, "Watermark", "MazClient", WATERMARK_LAYOUT);
        if (enabled(targetHealthModule) && hasTargetHealth) drawHudBox(graphics, client, "Target Health", targetHealthText, TARGET_HEALTH_LAYOUT);
        if (enabled(itemCounterModule) && client.player != null) drawHudBox(graphics, client, "Item Counter", itemCounterText, ITEM_COUNTER_LAYOUT);
        if (enabled(armorDurabilityModule) && client.player != null) drawHudBox(graphics, client, "Armor Durability", armorDurabilityText, ARMOR_DURABILITY_LAYOUT);
        if (enabled(compassModule) && client.player != null) drawHudBox(graphics, client, "Compass", compassText, COMPASS_LAYOUT);
        if (enabled(armorModule) && client.player != null) drawHudBox(graphics, client, "Armor HUD", armorHudText, ARMOR_LAYOUT);
        if (enabled(comboModule)) drawHudBox(graphics, client, "Combo Counter", comboText, COMBO_LAYOUT);
        if (enabled(reachModule)) drawHudBox(graphics, client, "Reach Display", reachText, REACH_LAYOUT);
        if (enabled(potionHudModule) && client.player != null) drawHudBox(graphics, client, "Potion HUD", potionText, POTION_LAYOUT);
    }

    private static void resolveModules() {
        if (modulesResolved) return;
        saturationModule = MazClient.MODULE_MANAGER.getModule("Saturation");
        fpsModule = MazClient.MODULE_MANAGER.getModule("FPS");
        memoryModule = MazClient.MODULE_MANAGER.getModule("Memory");
        coordinatesModule = MazClient.MODULE_MANAGER.getModule("Coordinates");
        pingModule = MazClient.MODULE_MANAGER.getModule("Ping");
        speedModule = MazClient.MODULE_MANAGER.getModule("Speed");
        directionModule = MazClient.MODULE_MANAGER.getModule("Direction");
        clockModule = MazClient.MODULE_MANAGER.getModule("Clock");
        sessionModule = MazClient.MODULE_MANAGER.getModule("Session Timer");
        cpsModule = MazClient.MODULE_MANAGER.getModule("CPS");
        keystrokesModule = MazClient.MODULE_MANAGER.getModule("Keystrokes");
        potCounterModule = MazClient.MODULE_MANAGER.getModule("PotCounter");
        watermarkModule = MazClient.MODULE_MANAGER.getModule("Watermark");
        targetHealthModule = MazClient.MODULE_MANAGER.getModule("Target Health");
        itemCounterModule = MazClient.MODULE_MANAGER.getModule("Item Counter");
        armorDurabilityModule = MazClient.MODULE_MANAGER.getModule("Armor Durability");
        compassModule = MazClient.MODULE_MANAGER.getModule("Compass");
        armorModule = MazClient.MODULE_MANAGER.getModule("Armor HUD");
        comboModule = MazClient.MODULE_MANAGER.getModule("Combo Counter");
        reachModule = MazClient.MODULE_MANAGER.getModule("Reach Display");
        potionHudModule = MazClient.MODULE_MANAGER.getModule("Potion HUD");
        modulesResolved = true;
    }

    private static void refreshRealtimeText(Minecraft client) {
        if (enabled(fpsModule)) fpsText = "FPS: " + client.getFps();
        if (enabled(cpsModule)) cpsText = "CPS: L " + CpsModule.getLeftCps() + " | R " + CpsModule.getRightCps();
        if (enabled(comboModule)) comboText = "Combo: " + CombatStats.getCombo();
        if (enabled(reachModule)) reachText = "Reach: " + fixed2(CombatStats.getLastReach());

        if (enabled(targetHealthModule) && client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            hasTargetHealth = true;
            targetHealthText = living.getName().getString() + ": "
                    + fixed1(Math.max(0.0F, living.getHealth())) + " / "
                    + fixed1(living.getMaxHealth()) + " HP";
        } else {
            hasTargetHealth = false;
        }

        if (client.player == null) return;

        if (enabled(coordinatesModule)) {
            int x = (int) Math.floor(client.player.getX()), y = (int) Math.floor(client.player.getY()), z = (int) Math.floor(client.player.getZ());
            coordinatesText = buildCoordinatesText(client, x, y, z);
        }
        if (enabled(speedModule)) {
            double dx = client.player.getX() - client.player.xOld, dz = client.player.getZ() - client.player.zOld;
            speedText = "Speed: " + fixed2(Math.sqrt(dx * dx + dz * dz) * 20.0) + " b/s";
        }
        if (enabled(directionModule)) {
            float yaw = client.player.getYRot(), pitch = client.player.getXRot();
            directionText = "Facing: " + facingName(yaw) + " | Yaw: " + fixed1(yaw) + "° | Pitch: " + fixed1(pitch) + "°";
        }
        if (enabled(compassModule)) {
            float yaw = ((client.player.getYRot() % 360.0F) + 360.0F) % 360.0F;
            compassText = compassName(yaw) + " " + Math.round(yaw) + "°";
        }
    }

    private static void refreshInventoryText(Minecraft client) {
        boolean countPots = enabled(potCounterModule);
        boolean countHeld = enabled(itemCounterModule);
        if (!countPots && !countHeld) return;

        ItemStack held = client.player.getMainHandItem();
        int potionCount = 0;
        int heldCount = 0;
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            if (countPots && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))) {
                potionCount += stack.getCount();
            }
            if (countHeld && !held.isEmpty() && stack.is(held.getItem())) {
                heldCount += stack.getCount();
            }
        }

        if (countPots) potCounterText = "Pots: " + potionCount;
        if (countHeld) itemCounterText = buildItemCounterText(held, heldCount);
    }

    private static void refreshEquipmentText(Minecraft client) {
        if (enabled(armorDurabilityModule)) armorDurabilityText = buildArmorDurabilityText(client);
        if (enabled(armorModule)) armorHudText = buildArmorHudText(client);
    }

    private static void refreshPotionText(Minecraft client) {
        if (enabled(potionHudModule)) potionText = potionHudText(client);
    }

    private static void refreshPingText(Minecraft client) {
        if (!enabled(pingModule) || !(pingModule instanceof PingModule ping) || client.getConnection() == null) return;
        int livePing = LivePingProbe.getLatestPingMs();
        if (livePing >= 0) {
            ping.sample(livePing);
            pingText = ping.getDisplayText();
            return;
        }

        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info != null) {
            ping.sample(info.getLatency());
            pingText = ping.getDisplayText();
        }
    }

    private static void resetPlayerTelemetry() {
        pingText = "Ping: -- ms";
        potCounterText = "Pots: 0";
        coordinatesText = "XYZ: --";
        speedText = "Speed: -- b/s";
        directionText = "Facing: --";
        itemCounterText = "Item: Empty";
        armorDurabilityText = "Armor: none";
        compassText = "--";
        armorHudText = "Helmet Empty | Chest Empty | Legs Empty | Boots Empty";
        potionText = "Effects: none";
    }

    private static String buildCoordinatesText(Minecraft client, int x, int y, int z) {
        String text = "XYZ: " + x + " / " + y + " / " + z + " | Chunk: " + Math.floorDiv(x, 16) + " / " + Math.floorDiv(z, 16)
                + " | Local: " + Math.floorMod(x, 16) + " / " + Math.floorMod(z, 16);
        if (client.level == null) return text;
        if (Level.OVERWORLD.equals(client.level.dimension())) {
            return text + " | Nether: " + Math.floorDiv(x, 8) + " / " + Math.floorDiv(z, 8);
        }
        if (Level.NETHER.equals(client.level.dimension())) {
            return text + " | Overworld: " + ((long) x * 8L) + " / " + ((long) z * 8L);
        }
        return text;
    }

    private static void refreshSlowText(Minecraft client, long now) {
        if (enabled(memoryModule)) {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory(), max = runtime.maxMemory();
            int percent = max > 0 ? (int) ((used * 100) / max) : 0;
            String pressure = percent >= 90 ? "HIGH" : percent >= 75 ? "WARN" : "OK";
            memoryAccent = percent >= 90 ? MEMORY_HIGH : percent >= 75 ? MEMORY_WARN : MEMORY_OK;
            memoryText = "RAM: " + used / 1024 / 1024 + " / " + max / 1024 / 1024 + " MB (" + percent + "%) | " + pressure;
        }
        if (enabled(clockModule)) clockText = "Time: " + LocalTime.now().format(CLOCK_FORMAT);
        if (enabled(sessionModule)) {
            long totalSeconds = Math.max(0L, now - MazClient.SESSION_START_MILLIS) / 1000L;
            long hours = totalSeconds / 3600L, minutes = (totalSeconds % 3600L) / 60L, seconds = totalSeconds % 60L;
            sessionText = hours > 0 ? "Session: " + hours + ":" + twoDigits(minutes) + ":" + twoDigits(seconds) : "Session: " + twoDigits(minutes) + ":" + twoDigits(seconds);
        }
    }

    private static String buildItemCounterText(ItemStack held, int total) {
        if (held.isEmpty()) return "Item: Empty";
        String text = held.getHoverName().getString() + ": " + total;
        if (held.isDamageableItem()) {
            int max = held.getMaxDamage(), remaining = Math.max(0, max - held.getDamageValue()), percent = max > 0 ? Math.round((remaining * 100.0F) / max) : 0;
            text += " | Durability: " + remaining + "/" + max + " (" + percent + "%)";
        }
        return text;
    }

    private static String buildArmorDurabilityText(Minecraft client) {
        int remaining = 0, maximum = 0, weakestPercent = 101;
        String weakestPiece = "";
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = client.player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int maxDamage = stack.getMaxDamage(), itemRemaining = Math.max(0, maxDamage - stack.getDamageValue()), itemPercent = maxDamage > 0 ? Math.round((itemRemaining * 100.0F) / maxDamage) : 0;
                maximum += maxDamage;
                remaining += itemRemaining;
                if (itemPercent < weakestPercent) { weakestPercent = itemPercent; weakestPiece = armorSlotName(slot); }
            }
        }
        int percent = maximum > 0 ? Math.round((remaining * 100.0F) / maximum) : 0;
        return maximum > 0 ? "Armor: " + percent + "% | Weakest: " + weakestPiece + " " + weakestPercent + "%" : "Armor: none";
    }

    private static void drawSaturationOnHungerBar(GuiGraphicsExtractor graphics,Minecraft client){float saturation=Math.max(0.0F,Math.min(20.0F,client.player.getFoodData().getSaturationLevel()));if(saturation<=0.0F)return;int centerX=client.getWindow().getGuiScaledWidth()/2,hungerY=client.getWindow().getGuiScaledHeight()-39;for(int i=0;i<10;i++){float points=saturation-i*2.0F;if(points<=0.0F)break;int iconX=centerX+91-i*8-9,y=hungerY+7;if(points>=2.0F)graphics.fill(iconX+1,y,iconX+8,y+2,SATURATION_FULL);else graphics.fill(iconX+4,y,iconX+8,y+2,SATURATION_HALF);}}
    private static boolean enabled(Module module){return module!=null&&module.isEnabled();}
    public static int previewWidth(Minecraft client,String moduleName){return moduleName.equalsIgnoreCase("Keystrokes")?64:client.font.width(previewText(moduleName))+12;}
    public static int previewHeight(String moduleName){return moduleName.equalsIgnoreCase("Keystrokes")?64:18;}
    public static void drawPreview(GuiGraphicsExtractor graphics,Minecraft client,String moduleName,int x,int y){if(moduleName.equalsIgnoreCase("Keystrokes")){int alpha=HudLayout.getOpacity("Keystrokes");drawKey(graphics,client,"W",x+22,y,false,20,20,alpha,keyLabelWidth(client,"W"));drawKey(graphics,client,"A",x,y+22,false,20,20,alpha,keyLabelWidth(client,"A"));drawKey(graphics,client,"S",x+22,y+22,true,20,20,alpha,keyLabelWidth(client,"S"));drawKey(graphics,client,"D",x+44,y+22,false,20,20,alpha,keyLabelWidth(client,"D"));drawKey(graphics,client,"LMB",x,y+44,false,30,20,alpha,keyLabelWidth(client,"LMB"));drawKey(graphics,client,"RMB",x+32,y+44,false,30,20,alpha,keyLabelWidth(client,"RMB"));}else if(moduleName.equalsIgnoreCase("Memory")){drawHudBoxPreview(graphics,client,moduleName,previewText(moduleName),new HudLayout.Position(x,y),MEMORY_OK);}else drawHudBoxPreview(graphics,client,moduleName,previewText(moduleName),new HudLayout.Position(x,y),ACCENT);}
    private static String previewText(String moduleName){return switch(moduleName){case "FPS"->"FPS: 120";case "Memory"->"RAM: 1024 / 4096 MB (25%) | OK";case "Coordinates"->"XYZ: 100 / 64 / -100 | Chunk: 6 / -7 | Local: 4 / 12 | Nether: 12 / -13";case "Ping"->"Ping: 42 ms";case "Speed"->"Speed: 4.20 b/s";case "Direction"->"Facing: North | Yaw: 180.0° | Pitch: -12.5°";case "Clock"->"Time: 12:34 PM";case "Session Timer"->"Session: 12:34";case "CPS"->"CPS: L 8 | R 5";case "PotCounter"->"Pots: 6";case "Watermark"->"MazClient";case "Target Health"->"Zombie: 18.0 / 20.0 HP";case "Item Counter"->"Diamond Pickaxe: 1 | Durability: 1087/1561 (70%)";case "Armor Durability"->"Armor: 82% | Weakest: Boots 34%";case "Compass"->"NW 315°";case "Armor HUD"->"Helmet Diamond Helmet 145/165 | Chest Diamond Chestplate 412/528 | Legs Diamond Leggings 198/225 | Boots Diamond Boots 31/195";case "Combo Counter"->"Combo: 4";case "Reach Display"->"Reach: 3.12";case "Potion HUD"->"Effects: Speed II 1:23 | Strength 0:42";default->moduleName;};}
    private static String facingName(float yaw){float n=((yaw%360.0F)+360.0F)%360.0F;if(n>=315.0F||n<45.0F)return"South";if(n<135.0F)return"West";if(n<225.0F)return"North";return"East";}
    private static String compassName(float yaw){if(yaw>=337.5F||yaw<22.5F)return"S";if(yaw<67.5F)return"SW";if(yaw<112.5F)return"W";if(yaw<157.5F)return"NW";if(yaw<202.5F)return"N";if(yaw<247.5F)return"NE";if(yaw<292.5F)return"E";return"SE";}
    private static String armorSlotName(EquipmentSlot slot){return switch(slot){case HEAD->"Helmet";case CHEST->"Chest";case LEGS->"Legs";case FEET->"Boots";default->slot.getName();};}
    private static String buildArmorHudText(Minecraft client){StringBuilder text=new StringBuilder();for(EquipmentSlot slot:ARMOR_SLOTS){if(text.length()>0)text.append(" | ");ItemStack stack=client.player.getItemBySlot(slot);text.append(armorSlotName(slot)).append(' ');if(stack.isEmpty()){text.append("Empty");continue;}text.append(stack.getHoverName().getString()).append(' ');if(stack.isDamageableItem()){int max=stack.getMaxDamage(),remaining=Math.max(0,max-stack.getDamageValue());text.append(remaining).append('/').append(max);}else text.append("n/a");}return text.toString();}
    private static String potionHudText(Minecraft client){var effects=client.player.getActiveEffects();if(effects.isEmpty())return"Effects: none";StringBuilder text=new StringBuilder("Effects: ");int shown=0;for(MobEffectInstance effect:effects){if(shown>=3)break;if(shown>0)text.append(" | ");text.append(Component.translatable(effect.getDescriptionId()).getString());int level=effect.getAmplifier()+1;if(level>1)text.append(' ').append(effectLevel(level));text.append(' ').append(formatEffectDuration(effect.getDuration()));shown++;}if(effects.size()>shown)text.append(" | +").append(effects.size()-shown).append(" more");return text.toString();}
    private static String formatEffectDuration(int ticks){long totalSeconds=Math.max(0,ticks)/20L,longHours=totalSeconds/3600L,minutes=(totalSeconds%3600L)/60L,seconds=totalSeconds%60L;return longHours>0?longHours+":"+twoDigits(minutes)+":"+twoDigits(seconds):minutes+":"+twoDigits(seconds);}
    private static String effectLevel(int level){return switch(level){case 2->"II";case 3->"III";case 4->"IV";case 5->"V";case 6->"VI";case 7->"VII";case 8->"VIII";case 9->"IX";case 10->"X";default->Integer.toString(level);};}
    private static void drawKeystrokes(GuiGraphicsExtractor graphics,Minecraft client,HudLayout.Binding layout){ensureKeyLabelWidths(client);int x=layout.x(),y=layout.y(),key=20,gap=2,alpha=layout.opacity();drawKey(graphics,client,"W",x+key+gap,y,client.options.keyUp.isDown(),key,key,alpha,keyWidthW);int rowY=y+key+gap;drawKey(graphics,client,"A",x,rowY,client.options.keyLeft.isDown(),key,key,alpha,keyWidthA);drawKey(graphics,client,"S",x+key+gap,rowY,client.options.keyDown.isDown(),key,key,alpha,keyWidthS);drawKey(graphics,client,"D",x+(key+gap)*2,rowY,client.options.keyRight.isDown(),key,key,alpha,keyWidthD);int mouseY=rowY+key+gap,mouseWidth=key+10;drawKey(graphics,client,"LMB",x,mouseY,client.options.keyAttack.isDown(),mouseWidth,key,alpha,keyWidthLmb);drawKey(graphics,client,"RMB",x+mouseWidth+gap,mouseY,client.options.keyUse.isDown(),mouseWidth,key,alpha,keyWidthRmb);}
    private static void ensureKeyLabelWidths(Minecraft client){if(cachedKeyWidthFont==client.font)return;cachedKeyWidthFont=client.font;keyWidthW=client.font.width("W");keyWidthA=client.font.width("A");keyWidthS=client.font.width("S");keyWidthD=client.font.width("D");keyWidthLmb=client.font.width("LMB");keyWidthRmb=client.font.width("RMB");}
    private static int keyLabelWidth(Minecraft client,String label){ensureKeyLabelWidths(client);return switch(label){case"W"->keyWidthW;case"A"->keyWidthA;case"S"->keyWidthS;case"D"->keyWidthD;case"LMB"->keyWidthLmb;case"RMB"->keyWidthRmb;default->client.font.width(label);};}
    private static void drawKey(GuiGraphicsExtractor graphics,Minecraft client,String label,int x,int y,boolean pressed,int width,int height,int alpha,int labelWidth){graphics.fill(x,y,x+width,y+height,withAlpha(pressed?ACCENT:KEY_BG,alpha));int textX=x+(width-labelWidth)/2,textY=y+(height-8)/2;graphics.text(client.font,label,textX,textY,pressed?PRESSED_TEXT:adaptiveTextColor(alpha),false);}
    private static void drawHudBox(GuiGraphicsExtractor graphics,Minecraft client,String moduleName,String text,HudLayout.Binding layout){drawHudBox(graphics,client,moduleName,text,layout,ACCENT);}
    private static void drawHudBox(GuiGraphicsExtractor graphics,Minecraft client,String moduleName,String text,HudLayout.Binding layout,int accent){int alpha=layout.opacity(),width;HudWidthCacheEntry cached=HUD_WIDTH_CACHE.get(moduleName);if(cached==null||!cached.text().equals(text)){cached=new HudWidthCacheEntry(text,client.font.width(text)+12);HUD_WIDTH_CACHE.put(moduleName,cached);}width=cached.width();int x=layout.x(),y=layout.y();graphics.fill(x,y,x+width,y+18,withAlpha(BACKGROUND,alpha));graphics.fill(x,y,x+3,y+18,withAlpha(accent,alpha));graphics.text(client.font,text,x+7,y+6,adaptiveTextColor(alpha),false);}
    private static void drawHudBoxPreview(GuiGraphicsExtractor graphics,Minecraft client,String moduleName,String text,HudLayout.Position p,int accent){int alpha=HudLayout.getOpacity(moduleName),width=client.font.width(text)+12;graphics.fill(p.x(),p.y(),p.x()+width,p.y()+18,withAlpha(BACKGROUND,alpha));graphics.fill(p.x(),p.y(),p.x()+3,p.y()+18,withAlpha(accent,alpha));graphics.text(client.font,text,p.x()+7,p.y()+6,adaptiveTextColor(alpha),false);}
    private static String fixed1(double value){long scaled=Math.round(value*10.0),absolute=Math.abs(scaled);return(scaled<0?"-":"")+(absolute/10L)+"."+(absolute%10L);}
    private static String fixed2(double value){long scaled=Math.round(value*100.0),absolute=Math.abs(scaled),fraction=absolute%100L;return(scaled<0?"-":"")+(absolute/100L)+"."+(fraction<10L?"0":"")+fraction;}
    private static String twoDigits(long value){return value<10L?"0"+value:Long.toString(value);}
    private static int adaptiveTextColor(int alpha){int clamped=Math.max(0,Math.min(255,alpha));int channel=255-clamped;return 0xFF000000|(channel<<16)|(channel<<8)|channel;}
    private static int withAlpha(int color,int alpha){return(Math.max(0,Math.min(255,alpha))<<24)|(color&0x00FFFFFF);}
    private record HudWidthCacheEntry(String text,int width){}
}

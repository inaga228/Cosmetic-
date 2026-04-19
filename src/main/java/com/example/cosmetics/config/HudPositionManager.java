package com.example.cosmetics.config;

import net.minecraft.client.Minecraft;

/**
 * Stores the screen positions of all draggable HUD elements.
 * Positions are saved/loaded via ConfigManager.
 */
public final class HudPositionManager {

    private static final HudPositionManager INSTANCE = new HudPositionManager();
    public static HudPositionManager get() { return INSTANCE; }

    // Cosmetics HUD (top-left by default)
    private int cosmeticsX = 8;
    private int cosmeticsY = 8;

    // Target HUD (top-center by default — -1 means auto-center)
    private int targetX = -1;
    private int targetY = 10;

    private HudPositionManager() {}

    // ---- Cosmetics HUD -------------------------------------------------------

    public int getCosmeticsX() { return cosmeticsX; }
    public int getCosmeticsY() { return cosmeticsY; }

    public void setCosmeticsPos(int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        cosmeticsX = Math.max(0, Math.min(sw - 10, x));
        cosmeticsY = Math.max(0, Math.min(sh - 10, y));
    }

    // ---- Target HUD ----------------------------------------------------------

    /** Returns the X for the Target HUD, auto-centering if not set. */
    public int getTargetX(int hudWidth) {
        if (targetX < 0) {
            Minecraft mc = Minecraft.getInstance();
            return (mc.getWindow().getGuiScaledWidth() - hudWidth) / 2;
        }
        return targetX;
    }

    /** Raw stored X (may be -1 for auto). */
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }

    public void setTargetPos(int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        targetX = Math.max(0, Math.min(sw - 10, x));
        targetY = Math.max(0, Math.min(sh - 10, y));
    }

    public void resetTargetToCenter() { targetX = -1; }
}

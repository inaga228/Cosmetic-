package com.example.cosmetics.feature;

/**
 * Mutable settings bag for a feature.
 */
public final class FeatureSettings {
    public float colorR = 1.0F;
    public float colorG = 1.0F;
    public float colorB = 1.0F;

    public float size    = 1.0F;
    public float density = 1.0F;
    public float speed   = 1.0F;

    public int style = 0;
    public int count = 8;

    public float offsetX = 0.0F, offsetY = 0.0F, offsetZ = 0.0F;
    public float rotX    = 0.0F, rotY    = 0.0F, rotZ    = 0.0F;

    // ---- Kill Aura ----------------------------------------------------------
    public boolean killAuraAutoCrit      = false;
    public boolean killAuraTargetPlayers = false;
    public boolean killAuraTargetHostile = true;
    public boolean killAuraTargetPassive = false;
    public boolean killAuraAntiBot       = false;
    public int     killAuraRotMode       = 0; // 0=BodyTrack 1=FullLock 2=Silent
    public int     killAuraSortMode      = 0; // 0=Closest 1=LowestHP 2=HighestHP

    // ---- Trails -------------------------------------------------------------
    public boolean trailOnlyWhenMoving = false;
    public int     trailFadeMode       = 0; // 0=Solid 1=Fade 2=Rainbow

    // ---- Auras (Particles) --------------------------------------------------
    public boolean auraRotate          = true;
    public int     auraShape           = 0;  // 0=Orbit 1=Random 2=Pulse

    // ---- Wings --------------------------------------------------------------
    public boolean wingsOnlySprinting  = false;
    public boolean wingsFlapAnim       = true;

    // ---- Smooth Aim ---------------------------------------------------------
    public boolean smoothAimOnlyPlayers     = true;
    public boolean smoothAimNeedWeapon      = false;

    // ---- Auto Clicker -------------------------------------------------------
    public boolean clickerOnlyInRange       = false;

    // ---- Strafe -------------------------------------------------------------
    public int     strafeDirection          = 0; // 0=Left 1=Right 2=Random

    // ---- HUD ----------------------------------------------------------------
    public boolean hudOnlyWhenActive        = false;

    // ---- Auto Totem ---------------------------------------------------------
    public boolean totemShowAlert           = true;

    // ---- Legacy (kept for compat) -------------------------------------------
    public int style2     = 0;
    public int extraFlags = 0x0A;
    // -------------------------------------------------------------------------

    public int argb() {
        int r = clamp255((int)(colorR * 255));
        int g = clamp255((int)(colorG * 255));
        int b = clamp255((int)(colorB * 255));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
}

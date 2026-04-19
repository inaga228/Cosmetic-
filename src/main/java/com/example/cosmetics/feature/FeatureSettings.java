package com.example.cosmetics.feature;

/**
 * Mutable settings bag for a feature.
 * Not every field is used by every feature; see {@link FeatureType#caps}.
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

    // ---- Kill Aura boolean toggles -------------------------------------------
    // Weapon cooldown gate: attack ONLY when the weapon is fully charged (always ON now)
    // autoCrit: micro-jump before each swing for guaranteed crit
    public boolean killAuraAutoCrit   = false;
    // Target filters
    public boolean killAuraTargetPlayers  = false;
    public boolean killAuraTargetHostile  = true;
    public boolean killAuraTargetPassive  = false;
    // Anti-bot filter
    public boolean killAuraAntiBot        = false;
    // Rotation mode (stored as int for CycleButton)
    // 0 = BODY_TRACK, 1 = FULL_LOCK, 2 = SILENT
    public int killAuraRotMode = 0;
    // Sort mode
    // 0 = closest, 1 = lowest HP, 2 = highest HP
    public int killAuraSortMode = 0;
    // -------------------------------------------------------------------------

    // ---- Smooth Aim ---------------------------------------------------------
    // No toggles needed here, kept for FOV (size) only.
    // -------------------------------------------------------------------------

    // ---- Legacy extraFlags (kept for compat, but KA now uses booleans above) --
    public int style2      = 0;
    public int extraFlags  = 0x0A;
    // -------------------------------------------------------------------------

    public int argb() {
        int r = clamp255((int)(colorR * 255));
        int g = clamp255((int)(colorG * 255));
        int b = clamp255((int)(colorB * 255));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
}

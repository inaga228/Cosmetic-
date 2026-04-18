package com.example.cosmetics.feature;

/**
 * All configurable features in the mod.
 * Each value groups which settings fields are relevant for its settings screen.
 */
public enum FeatureType {
    // Trails (now rendered as a 3D ribbon, not particles)
    RAINBOW_TRAIL("Rainbow Trail",  Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.STYLE),
    FLAME_TRAIL  ("Flame Trail",    Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.STYLE),
    GALAXY_TRAIL ("Galaxy Trail",   Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.STYLE),

    // Auras / particles around player
    AURA        ("Aura",            Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),
    SNOW_AURA   ("Snow Aura",       Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),
    HEART_AURA  ("Hearts",          Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),

    // Hat (hidden in first-person to keep view clear)
    CHINA_HAT   ("China Hat",       Category.HAT,     Caps.COLOR | Caps.SIZE | Caps.STYLE | Caps.OFFSET),

    // Wings (flap anim driven by player speed)
    DRAGON_WINGS ("Dragon Wings",   Category.WINGS,   Caps.COLOR | Caps.SIZE | Caps.SPEED | Caps.STYLE | Caps.OFFSET),

    // Hit effects (more styles: slash, stars, crit, ripple, shards, bolt)
    HIT_EFFECT  ("Hit Effect",      Category.COMBAT,  Caps.COLOR | Caps.SIZE | Caps.COUNT | Caps.STYLE),

    // HUDs — now with multiple style/themes
    COSMETICS_HUD ("Cosmetics HUD", Category.HUD,     Caps.COLOR | Caps.STYLE),
    TARGET_HUD    ("Target HUD",    Category.HUD,     Caps.COLOR | Caps.STYLE),

    // View model / animations
    VIEW_MODEL       ("View Model",          Category.ANIM, Caps.OFFSET | Caps.ROTATION),
    CUSTOM_ATTACK    ("Custom Attack Anim",  Category.ANIM, Caps.SIZE),
    CUSTOM_PLACE     ("Custom Place Anim",   Category.ANIM, Caps.SIZE);

    public enum Category { TRAILS, PARTICLES, HAT, WINGS, COMBAT, HUD, ANIM }

    /** Bitmask of which settings fields this feature uses. */
    public static final class Caps {
        public static final int COLOR    = 1 << 0;
        public static final int SIZE     = 1 << 1;
        public static final int DENSITY  = 1 << 2;
        public static final int SPEED    = 1 << 3;
        public static final int STYLE    = 1 << 4;
        public static final int COUNT    = 1 << 5;
        public static final int OFFSET   = 1 << 6;
        public static final int ROTATION = 1 << 7;
    }

    public final String displayName;
    public final Category category;
    public final int caps;

    FeatureType(String displayName, Category category, int caps) {
        this.displayName = displayName;
        this.category = category;
        this.caps = caps;
    }

    public boolean has(int cap) { return (caps & cap) != 0; }
}

package com.example.cosmetics.feature;

/**
 * All configurable features in the mod.
 * Each value groups which settings fields are relevant for its settings screen.
 */
public enum FeatureType {
    // Trails
    RAINBOW_TRAIL("Rainbow Trail",  Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),
    FLAME_TRAIL  ("Flame Trail",    Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),
    GALAXY_TRAIL ("Galaxy Trail",   Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),

    // Auras / particles around player
    AURA        ("Aura",            Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),
    SNOW_AURA   ("Snow Aura",       Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),
    HEART_AURA  ("Hearts",          Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED),

    // Movement / motion effects
    JUMP_CIRCLES  ("Jump Circles",   Category.EFFECTS, Caps.COLOR | Caps.SIZE | Caps.STYLE),
    LANDING_RING  ("Landing Ring",   Category.EFFECTS, Caps.COLOR | Caps.SIZE),

    // Hat
    CHINA_HAT   ("China Hat",       Category.HAT,     Caps.COLOR | Caps.SIZE | Caps.STYLE | Caps.OFFSET),

    // Hit effects
    HIT_EFFECT  ("Hit Effect",      Category.COMBAT,  Caps.COLOR | Caps.SIZE | Caps.COUNT | Caps.STYLE),

    // HUDs
    COSMETICS_HUD ("Cosmetics HUD", Category.HUD,     Caps.COLOR),
    TARGET_HUD    ("Target HUD",    Category.HUD,     Caps.COLOR),
    WATERMARK     ("Watermark",     Category.HUD,     Caps.COLOR | Caps.STYLE),

    // View model / animations
    VIEW_MODEL       ("View Model",          Category.ANIM, Caps.OFFSET | Caps.ROTATION),
    CUSTOM_ATTACK    ("Custom Attack Anim",  Category.ANIM, Caps.SIZE),
    CUSTOM_PLACE     ("Custom Place Anim",   Category.ANIM, Caps.SIZE);

    public enum Category { TRAILS, PARTICLES, EFFECTS, HAT, COMBAT, HUD, ANIM }

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

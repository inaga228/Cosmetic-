package com.example.cosmetics.feature;

public enum FeatureType {
    // Trails
    RAINBOW_TRAIL("Rainbow Trail",  Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.STYLE | Caps.BOOL),
    FLAME_TRAIL  ("Flame Trail",    Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.STYLE | Caps.BOOL),
    GALAXY_TRAIL ("Galaxy Trail",   Category.TRAILS,  Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.STYLE | Caps.BOOL),

    // Auras
    AURA        ("Aura",            Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.BOOL),
    SNOW_AURA   ("Snow Aura",       Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.BOOL),
    HEART_AURA  ("Hearts",          Category.PARTICLES, Caps.COLOR | Caps.SIZE | Caps.DENSITY | Caps.SPEED | Caps.BOOL),

    // Hat
    CHINA_HAT   ("China Hat",       Category.HAT,     Caps.COLOR | Caps.SIZE | Caps.STYLE | Caps.OFFSET),

    // Wings
    DRAGON_WINGS ("Wings",          Category.WINGS,   Caps.COLOR | Caps.SIZE | Caps.SPEED | Caps.STYLE | Caps.OFFSET | Caps.BOOL),

    // Effects
    JUMP_CIRCLES  ("Jump Circles",  Category.EFFECTS, Caps.COLOR | Caps.SIZE | Caps.STYLE | Caps.SPEED),
    LANDING_RING  ("Landing Ring",  Category.EFFECTS, Caps.COLOR | Caps.SIZE | Caps.STYLE),

    // Hit effects
    HIT_EFFECT  ("Hit Effect",      Category.COMBAT,  Caps.COLOR | Caps.SIZE | Caps.COUNT | Caps.STYLE),

    // HUDs
    COSMETICS_HUD ("Cosmetics HUD", Category.HUD,     Caps.COLOR | Caps.STYLE | Caps.BOOL),
    TARGET_HUD    ("Target HUD",    Category.HUD,     Caps.COLOR | Caps.STYLE | Caps.BOOL),

    // Animations
    VIEW_MODEL       ("View Model",         Category.ANIM, Caps.OFFSET | Caps.ROTATION),
    CUSTOM_ATTACK    ("Custom Attack Anim", Category.ANIM, Caps.SIZE),
    CUSTOM_PLACE     ("Custom Place Anim",  Category.ANIM, Caps.SIZE),

    // Utility
    AUTO_SPRINT  ("Auto Sprint",    Category.UTILITY, 0),
    AUTO_JUMP    ("Auto Jump",      Category.UTILITY, 0),
    AUTO_SNEAK   ("Auto Sneak",     Category.UTILITY, 0),
    FULLBRIGHT   ("Fullbright",     Category.UTILITY, 0),
    AUTO_TOTEM   ("Auto Totem",     Category.UTILITY, Caps.COUNT | Caps.BOOL),

    // Combat
    NO_FIRE_OVERLAY ("No Fire Overlay", Category.COMBAT, 0),
    KILL_AURA    ("Kill Aura",    Category.COMBAT, Caps.SIZE | Caps.BOOL),
    CRIT         ("Crit",         Category.COMBAT, 0),
    AUTO_CLICKER ("Auto Clicker", Category.COMBAT, Caps.COUNT | Caps.SPEED | Caps.BOOL),
    SMOOTH_AIM   ("Smooth Aim",   Category.COMBAT, Caps.SIZE | Caps.SPEED | Caps.BOOL),
    STRAFE       ("Strafe",       Category.COMBAT, Caps.SPEED | Caps.BOOL),
    ANTI_BOT     ("Anti Bot",     Category.COMBAT, 0),
    AUTO_POT     ("Auto Pot",     Category.COMBAT, Caps.COUNT),
    AUTO_GAP     ("Auto Gap",     Category.COMBAT, Caps.COUNT);

    public enum Category { TRAILS, PARTICLES, HAT, WINGS, EFFECTS, COMBAT, HUD, ANIM, UTILITY }

    public static final class Caps {
        public static final int COLOR    = 1 << 0;
        public static final int SIZE     = 1 << 1;
        public static final int DENSITY  = 1 << 2;
        public static final int SPEED    = 1 << 3;
        public static final int STYLE    = 1 << 4;
        public static final int COUNT    = 1 << 5;
        public static final int OFFSET   = 1 << 6;
        public static final int ROTATION = 1 << 7;
        public static final int BOOL     = 1 << 8;
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

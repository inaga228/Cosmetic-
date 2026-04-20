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
    CAPE         ("Cape",           Category.WINGS,   Caps.COLOR | Caps.SIZE | Caps.SPEED | Caps.STYLE | Caps.BOOL),

    // Effects
    JUMP_CIRCLES  ("Jump Circles",  Category.EFFECTS, Caps.COLOR | Caps.SIZE | Caps.STYLE | Caps.SPEED),
    LANDING_RING  ("Landing Ring",  Category.EFFECTS, Caps.COLOR | Caps.SIZE | Caps.STYLE),

    // HUDs
    COSMETICS_HUD ("Cosmetics HUD", Category.HUD,     Caps.COLOR | Caps.STYLE | Caps.BOOL),
    TARGET_HUD    ("Target HUD",    Category.HUD,     Caps.COLOR | Caps.STYLE | Caps.BOOL),

    // Animations
    VIEW_MODEL       ("View Model",         Category.ANIM, Caps.OFFSET | Caps.ROTATION),
    CUSTOM_ATTACK    ("Custom Attack Anim", Category.ANIM, Caps.SIZE),
    CUSTOM_PLACE     ("Custom Place Anim",  Category.ANIM, Caps.SIZE | Caps.COUNT),

    // Utility
    AUTO_SPRINT  ("Auto Sprint",    Category.UTILITY, 0),
    AUTO_JUMP    ("Auto Jump",      Category.UTILITY, 0),
    AUTO_SNEAK   ("Auto Sneak",     Category.UTILITY, 0),
    FULLBRIGHT   ("Fullbright",     Category.UTILITY, 0),
    AUTO_TOTEM   ("Auto Totem",     Category.UTILITY, Caps.COUNT | Caps.BOOL),
    AUTO_POT     ("Auto Pot",       Category.UTILITY, Caps.COUNT),
    AUTO_GAP     ("Auto Gap",       Category.UTILITY, Caps.COUNT),

    // Combat
    NO_FIRE_OVERLAY ("No Fire Overlay", Category.COMBAT, 0),
    KILL_AURA    ("Kill Aura",    Category.COMBAT, Caps.SIZE | Caps.BOOL | Caps.KILLAURA),
    CRIT         ("Crit",         Category.COMBAT, 0),
    AUTO_CLICKER ("Auto Clicker", Category.COMBAT, Caps.COUNT | Caps.SPEED | Caps.BOOL),
    SMOOTH_AIM   ("Smooth Aim",   Category.COMBAT, Caps.SIZE | Caps.SPEED | Caps.BOOL),
    STRAFE       ("Strafe",       Category.COMBAT, Caps.SPEED | Caps.BOOL | Caps.STRAFE),
    ANTI_BOT     ("Anti Bot",     Category.COMBAT, 0),
    HIT_EFFECT   ("Hit Effect",   Category.COMBAT, Caps.COLOR | Caps.SIZE | Caps.COUNT | Caps.STYLE),
    TRIGGER_BOT  ("Trigger Bot",  Category.COMBAT, Caps.TRIGGER),
    BOW_AIMBOT   ("Bow Aimbot",   Category.COMBAT, Caps.BOWAIMBOT),
    HITBOX       ("Hitbox",       Category.COMBAT, Caps.HITBOX),

    // Optimizations
    FAR_VIEW         ("Far View",         Category.OPTIM, 0),
    FPS_BOOST        ("FPS Boost",        Category.OPTIM, 0),
    FAST_GRAPHICS    ("Fast Graphics",    Category.OPTIM, 0),
    NO_CLOUDS        ("No Clouds",        Category.OPTIM, 0),
    NO_ENTITY_SHADOW ("No Entity Shadow", Category.OPTIM, 0);

    public enum Category { TRAILS, PARTICLES, HAT, WINGS, EFFECTS, COMBAT, HUD, ANIM, UTILITY, OPTIM }

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
        public static final int KILLAURA = 1 << 9;
        public static final int STRAFE   = 1 << 10;
        public static final int ESP      = 1 << 11;
        public static final int TRIGGER  = 1 << 12;
        public static final int BOWAIMBOT= 1 << 13;
        public static final int HITBOX   = 1 << 14;
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

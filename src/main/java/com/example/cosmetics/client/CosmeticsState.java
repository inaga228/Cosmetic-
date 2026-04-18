package com.example.cosmetics.client;

import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Client-only singleton: which features are enabled and their per-feature
 * settings. No persistence.
 */
public final class CosmeticsState {
    private static final CosmeticsState INSTANCE = new CosmeticsState();
    public static CosmeticsState get() { return INSTANCE; }

    private final Set<FeatureType> enabled = EnumSet.noneOf(FeatureType.class);
    private final Map<FeatureType, FeatureSettings> settings = new EnumMap<>(FeatureType.class);

    private CosmeticsState() {
        // Sensible defaults for the features that need non-default values.
        for (FeatureType f : FeatureType.values()) {
            FeatureSettings s = new FeatureSettings();
            applyDefaults(f, s);
            settings.put(f, s);
        }
        // Turn the HUD on by default so users see something immediately.
        enabled.add(FeatureType.COSMETICS_HUD);
        enabled.add(FeatureType.TARGET_HUD);
    }

    private static void applyDefaults(FeatureType f, FeatureSettings s) {
        switch (f) {
            // Trails (3D ribbon)
            case RAINBOW_TRAIL: s.size = 0.55F; s.density = 1.0F; break;
            case FLAME_TRAIL:   s.colorR = 1.0F; s.colorG = 0.55F; s.colorB = 0.1F; s.size = 0.60F; break;
            case GALAXY_TRAIL:  s.colorR = 0.70F; s.colorG = 0.50F; s.colorB = 1.00F; s.size = 0.45F; break;

            // Auras
            case AURA:          s.colorR = 0.55F; s.colorG = 0.35F; s.colorB = 1.00F; break;
            case SNOW_AURA:     s.colorR = 0.90F; s.colorG = 0.95F; s.colorB = 1.00F; break;
            case HEART_AURA:    s.colorR = 1.00F; s.colorG = 0.25F; s.colorB = 0.40F; break;

            // Hat
            case CHINA_HAT:     s.colorR = 0.85F; s.colorG = 0.20F; s.colorB = 0.25F;
                                s.offsetY = 0.0F; s.size = 1.0F; break;

            // Wings (small, pretty, lilac by default — Dragon style)
            case DRAGON_WINGS:  s.colorR = 0.60F; s.colorG = 0.30F; s.colorB = 1.00F;
                                s.size = 0.75F; s.speed = 1.0F; s.style = 0; break;

            // Jump rings — bright cyan, moderate size, DOUBLE style by default
            case JUMP_CIRCLES:  s.colorR = 0.30F; s.colorG = 0.85F; s.colorB = 1.00F;
                                s.size = 1.0F; s.speed = 1.0F; s.style = 2; break;
            // Landing rings — warm amber, slightly smaller, THICK by default
            case LANDING_RING:  s.colorR = 1.00F; s.colorG = 0.70F; s.colorB = 0.20F;
                                s.size = 0.9F; s.style = 1; break;

            // Combat
            case HIT_EFFECT:    s.colorR = 1.00F; s.colorG = 0.20F; s.colorB = 0.20F; s.count = 8; break;

            // HUDs — default to style 0 (classic neon-purple)
            case COSMETICS_HUD: s.colorR = 0.54F; s.colorG = 0.36F; s.colorB = 1.00F; s.style = 0; break;
            case TARGET_HUD:    s.colorR = 0.54F; s.colorG = 0.36F; s.colorB = 1.00F; s.style = 0; break;

            // Utility
            // AUTO_TOTEM: count = HP threshold in hearts (default 6 hearts = 12 HP)
            case AUTO_TOTEM:    s.count = 6; break;

            default: break;
        }
    }

    public boolean isOn(FeatureType f) { return enabled.contains(f); }
    public void toggle(FeatureType f) { if (!enabled.remove(f)) enabled.add(f); }
    public FeatureSettings settings(FeatureType f) { return settings.get(f); }
    public Set<FeatureType> active() { return enabled; }
}

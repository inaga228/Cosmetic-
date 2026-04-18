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
        // Turn the HUD + watermark on by default so users see something immediately.
        enabled.add(FeatureType.COSMETICS_HUD);
        enabled.add(FeatureType.TARGET_HUD);
        enabled.add(FeatureType.WATERMARK);
        enabled.add(FeatureType.JUMP_CIRCLES);
    }

    private static void applyDefaults(FeatureType f, FeatureSettings s) {
        switch (f) {
            case RAINBOW_TRAIL: s.size = 0.35F; s.density = 1.5F; break;
            case FLAME_TRAIL:   s.colorR = 1.0F; s.colorG = 0.55F; s.colorB = 0.1F; s.size = 0.4F; break;
            case GALAXY_TRAIL:  s.colorR = 0.7F; s.colorG = 0.5F; s.colorB = 1.0F; s.size = 0.25F; break;
            case AURA:          s.colorR = 0.55F; s.colorG = 0.35F; s.colorB = 1.0F; break;
            case SNOW_AURA:     s.colorR = 0.9F; s.colorG = 0.95F; s.colorB = 1.0F; break;
            case HEART_AURA:    s.colorR = 1.0F; s.colorG = 0.25F; s.colorB = 0.4F; break;
            case CHINA_HAT:     s.colorR = 0.85F; s.colorG = 0.2F; s.colorB = 0.25F;
                                s.offsetY = 0.0F; s.size = 1.0F; break;
            case HIT_EFFECT:    s.colorR = 1.0F; s.colorG = 0.2F; s.colorB = 0.2F; s.count = 8; break;
            case JUMP_CIRCLES:  s.colorR = 0.55F; s.colorG = 0.35F; s.colorB = 1.0F; s.size = 1.0F; s.style = 0; break;
            case LANDING_RING:  s.colorR = 1.0F;  s.colorG = 0.8F;  s.colorB = 0.3F; s.size = 1.0F; break;
            case WATERMARK:     s.colorR = 0.55F; s.colorG = 0.35F; s.colorB = 1.0F; s.style = 0; break;
            default: break;
        }
    }

    public boolean isOn(FeatureType f) { return enabled.contains(f); }
    public void toggle(FeatureType f) { if (!enabled.remove(f)) enabled.add(f); }
    public FeatureSettings settings(FeatureType f) { return settings.get(f); }
    public Set<FeatureType> active() { return enabled; }
}

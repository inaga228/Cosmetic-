package com.example.cosmetics.client;

import java.util.EnumSet;
import java.util.Set;

/**
 * Singleton state for which cosmetic effects are currently active.
 * Client-only; no persistence (could be added later via a config file).
 */
public final class CosmeticsState {
    public enum Trail { RAINBOW, FLAME, GALAXY }
    public enum Aura  { AURA, SNOW, HEARTS }
    public enum Hat   { CHINA }

    private static final CosmeticsState INSTANCE = new CosmeticsState();
    public static CosmeticsState get() { return INSTANCE; }

    private final Set<Trail> trails = EnumSet.noneOf(Trail.class);
    private final Set<Aura>  auras  = EnumSet.noneOf(Aura.class);
    private final Set<Hat>   hats   = EnumSet.noneOf(Hat.class);
    private boolean hudEnabled = true;

    public boolean isTrailOn(Trail t) { return trails.contains(t); }
    public boolean isAuraOn (Aura  a) { return auras.contains(a); }
    public boolean isHatOn  (Hat   h) { return hats.contains(h); }
    public boolean isHudEnabled()      { return hudEnabled; }

    public void toggleTrail(Trail t) { if (!trails.remove(t)) trails.add(t); }
    public void toggleAura (Aura  a) { if (!auras.remove(a))  auras.add(a);  }
    public void toggleHat  (Hat   h) { if (!hats.remove(h))   hats.add(h);   }
    public void toggleHud  ()        { hudEnabled = !hudEnabled; }

    public Set<Trail> activeTrails() { return trails; }
    public Set<Aura>  activeAuras()  { return auras; }
    public Set<Hat>   activeHats()   { return hats; }
}

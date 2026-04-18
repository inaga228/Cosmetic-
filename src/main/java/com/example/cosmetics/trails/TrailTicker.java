package com.example.cosmetics.trails;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 3D trail feeder. Tick records the player's position into
 * {@link TrailHistory} so {@link com.example.cosmetics.render.TrailRenderer}
 * can draw a continuous ribbon behind the player.
 *
 * <p>This replaces the old per-tick particle emission — trails are now a true
 * 3D ribbon rather than a cloud of particles.</p>
 */
public final class TrailTicker {

    public static void tick(PlayerEntity player) {
        if (player == null) { TrailHistory.reset(); return; }

        CosmeticsState s = CosmeticsState.get();
        boolean any = s.isOn(FeatureType.RAINBOW_TRAIL)
                   || s.isOn(FeatureType.FLAME_TRAIL)
                   || s.isOn(FeatureType.GALAXY_TRAIL);

        if (!any) {
            // Let the existing ribbon fade out naturally, then drop points.
            TrailHistory.ageOut(1500L);
            return;
        }

        double x = player.getX();
        double y = player.getY() + 0.1; // near feet
        double z = player.getZ();
        TrailHistory.push(x, y, z);
        TrailHistory.ageOut(1500L);
    }

    private TrailTicker() {}
}

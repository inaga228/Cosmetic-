package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;

/**
 * Temporarily pins the game's gamma to a very high value so caves and dark
 * areas are bright. When the feature is toggled off we restore the value
 * the user had before we touched it.
 *
 * We don't write to the options file — gamma is only adjusted in memory.
 * Vanilla caps gamma to 1.0 in the options UI but the field can be set
 * higher at runtime.
 */
public final class FullBright {

    private static final double BRIGHT_GAMMA = 16.0;

    private static boolean applied = false;
    private static double savedGamma = 1.0;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        GameSettings opts = mc.options;
        if (opts == null) return;

        boolean on = CosmeticsState.get().isOn(FeatureType.FULL_BRIGHT);

        if (on && !applied) {
            savedGamma = opts.gamma;
            opts.gamma = BRIGHT_GAMMA;
            applied = true;
        } else if (!on && applied) {
            opts.gamma = savedGamma;
            applied = false;
        }
    }

    private FullBright() {}
}

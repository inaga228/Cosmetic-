package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;

/**
 * Forces the local player into sprinting mode when moving forward. Does
 * nothing while the player is sneaking, flying, standing still, or blind
 * (sprint is disallowed by vanilla in those states anyway — we just try,
 * the server rejects if invalid).
 */
public final class AutoSprint {

    public static void tick() {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_SPRINT)) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (p.isSpectator()) return;

        // Only sprint if the forward key is held and we can physically sprint.
        if (!mc.options.keyUp.isDown()) return;
        if (p.isShiftKeyDown() || p.isCrouching()) return;
        if (p.isSprinting()) return;
        if (p.getFoodData().getFoodLevel() <= 6) return;    // vanilla requires >6
        if (p.isUsingItem()) return;

        p.setSprinting(true);
    }

    private AutoSprint() {}
}

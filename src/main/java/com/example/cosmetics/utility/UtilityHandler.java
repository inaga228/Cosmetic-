package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.potion.Effects;

/**
 * Handles all non-visual utility features:
 *  - Auto Sprint
 *  - Auto Jump
 *  - Auto Sneak
 *  - Fullbright
 *  - No Fall Damage
 *  - Step Assist
 *
 * Called once per client tick from ClientEvents.
 */
public final class UtilityHandler {

    private static final UtilityHandler INSTANCE = new UtilityHandler();
    public static UtilityHandler get() { return INSTANCE; }

    // Fullbright: remember original gamma so we can restore it on disable.
    private float originalGamma = -1F;
    private boolean fullbrightActive = false;

    // Step assist: remember original step height.
    private float originalStepHeight = -1F;
    private boolean stepAssistActive = false;

    private UtilityHandler() {}

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientPlayerEntity player = mc.player;
        CosmeticsState state = CosmeticsState.get();

        tickAutoSprint(player, state);
        tickAutoJump(player, state);
        tickAutoSneak(player, state);
        tickFullbright(mc, state);
        tickNoFall(player, state);
        tickStepAssist(player, state);
    }

    // -------------------------------------------------------------------------
    // Auto Sprint — constantly hold the sprint key as long as player is moving.
    // -------------------------------------------------------------------------
    private void tickAutoSprint(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_SPRINT)) return;

        // Don't sprint while sneaking, in water/lava or when blind.
        if (player.isCrouching()) return;
        if (player.isInWater() || player.isInLava()) return;
        if (player.hasEffect(Effects.BLINDNESS)) return;

        // Check if the player is trying to move forward.
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyUp.isDown()) {
            player.setSprinting(true);
        }
    }

    // -------------------------------------------------------------------------
    // Auto Jump — jump continuously while the player is on the ground and moving.
    // -------------------------------------------------------------------------
    private void tickAutoJump(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_JUMP)) return;
        if (!player.isOnGround()) return;
        if (player.isCrouching()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean moving = mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();

        if (moving) {
            player.jumpFromGround();
        }
    }

    // -------------------------------------------------------------------------
    // Auto Sneak — hold sneak while the player is not moving.
    // -------------------------------------------------------------------------
    private void tickAutoSneak(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_SNEAK)) return;

        Minecraft mc = Minecraft.getInstance();
        boolean moving = mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();

        // Simulate sneak when standing still (not when moving).
        PlayerAbilities abilities = player.abilities;
        if (!moving && !abilities.flying) {
            // We set the player input directly; this emulates holding shift.
            mc.options.keyShift.setDown(true);
        } else {
            // Only release if auto-sneak set it (keyShift could also be held by user).
            // We simply let Minecraft's normal input handle it when moving.
        }
    }

    // -------------------------------------------------------------------------
    // Fullbright — set gamma to max while enabled, restore on disable.
    // -------------------------------------------------------------------------
    private void tickFullbright(Minecraft mc, CosmeticsState state) {
        boolean wantOn = state.isOn(FeatureType.FULLBRIGHT);

        if (wantOn && !fullbrightActive) {
            // Turning on: save original gamma and apply fullbright value.
            originalGamma = (float)(double) mc.options.gamma;
            mc.options.gamma = 16.0D; // 16 is well above 1.0 — effectively fullbright.
            fullbrightActive = true;
        } else if (!wantOn && fullbrightActive) {
            // Turning off: restore saved gamma.
            if (originalGamma >= 0) {
                mc.options.gamma = originalGamma;
            }
            fullbrightActive = false;
        }
    }

    // -------------------------------------------------------------------------
    // No Fall Damage — reset fall distance every tick.
    // -------------------------------------------------------------------------
    private void tickNoFall(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.NO_FALL)) return;
        // Resetting fallDistance to 0 every tick means the server never
        // registers enough distance to deal damage.
        player.fallDistance = 0F;
    }

    // -------------------------------------------------------------------------
    // Step Assist — raise stepHeight so the player walks up 1-block steps
    //               without jumping (like horses / boats).
    // -------------------------------------------------------------------------
    private void tickStepAssist(ClientPlayerEntity player, CosmeticsState state) {
        boolean wantOn = state.isOn(FeatureType.STEP_ASSIST);

        // Read configured step height from settings (size slider, default 1.0 = 1 block).
        float configuredHeight = CosmeticsState.get().settings(FeatureType.STEP_ASSIST).size;
        float targetHeight = Math.max(0.6F, Math.min(2.0F, configuredHeight));

        if (wantOn && !stepAssistActive) {
            originalStepHeight = player.maxUpStep;
            player.maxUpStep = targetHeight;
            stepAssistActive = true;
        } else if (wantOn) {
            // Update in case the slider changed.
            player.maxUpStep = targetHeight;
        } else if (!wantOn && stepAssistActive) {
            player.maxUpStep = (originalStepHeight >= 0F) ? originalStepHeight : 0.6F;
            stepAssistActive = false;
        }
    }
}

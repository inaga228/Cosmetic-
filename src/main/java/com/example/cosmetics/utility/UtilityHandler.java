package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/**
 * Handles all non-visual utility features:
 *  - Auto Sprint
 *  - Auto Jump
 *  - Auto Sneak
 *  - Fullbright
 *  - Auto Totem
 *  - Fast Place
 */
public final class UtilityHandler {

    private static final UtilityHandler INSTANCE = new UtilityHandler();
    public static UtilityHandler get() { return INSTANCE; }

    // Fullbright
    private float originalGamma = -1F;
    private boolean fullbrightActive = false;

    // Fast Place
    private int placeTimer = 0;

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
        tickAutoTotem(player, state);
        tickFastPlace(mc, player, state);
    }

    // -------------------------------------------------------------------------
    // Auto Sprint
    // -------------------------------------------------------------------------
    private void tickAutoSprint(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_SPRINT)) return;
        if (player.isCrouching()) return;
        if (player.isInWater() || player.isInLava()) return;
        if (player.hasEffect(Effects.BLINDNESS)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyUp.isDown()) {
            player.setSprinting(true);
        }
    }

    // -------------------------------------------------------------------------
    // Auto Jump
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
    // Auto Sneak
    // -------------------------------------------------------------------------
    private void tickAutoSneak(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_SNEAK)) return;

        Minecraft mc = Minecraft.getInstance();
        boolean moving = mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();

        PlayerAbilities abilities = player.abilities;
        if (!moving && !abilities.flying) {
            mc.options.keyShift.setDown(true);
        }
    }

    // -------------------------------------------------------------------------
    // Fullbright
    // -------------------------------------------------------------------------
    private void tickFullbright(Minecraft mc, CosmeticsState state) {
        boolean wantOn = state.isOn(FeatureType.FULLBRIGHT);

        if (wantOn && !fullbrightActive) {
            originalGamma = (float)(double) mc.options.gamma;
            mc.options.gamma = 16.0D;
            fullbrightActive = true;
        } else if (!wantOn && fullbrightActive) {
            if (originalGamma >= 0) mc.options.gamma = originalGamma;
            fullbrightActive = false;
        }
    }

    // -------------------------------------------------------------------------
    // Auto Totem
    // -------------------------------------------------------------------------
    private void tickAutoTotem(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_TOTEM)) return;

        int thresholdHearts = state.settings(FeatureType.AUTO_TOTEM).count;
        float thresholdHp = thresholdHearts * 2.0F;
        float currentHp = player.getHealth();

        ItemStack offhand = player.getItemInHand(Hand.OFF_HAND);
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;
        if (currentHp > thresholdHp) return;

        NonNullList<ItemStack> inv = player.inventory.items;
        int totemSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        if (totemSlot == -1) return;

        ItemStack totem = inv.get(totemSlot);
        ItemStack currentOffhand = player.getItemInHand(Hand.OFF_HAND).copy();

        player.inventory.offhand.set(0, totem.copy());
        inv.set(totemSlot, currentOffhand.isEmpty() ? ItemStack.EMPTY : currentOffhand);
    }

    // -------------------------------------------------------------------------
    // Fast Place
    //
    // Vanilla Minecraft enforces a rightClickDelay of 4 ticks between block
    // placements. We bypass this by zeroing rightClickDelay and manually
    // calling processRightClick every N ticks (N = speed field, min 1).
    //
    // When the interval is 1 (Max) — places every single client tick.
    // -------------------------------------------------------------------------
    private void tickFastPlace(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.FAST_PLACE)) return;

        // PRM button must be held.
        if (!mc.options.keyUse.isDown()) {
            placeTimer = 0;
            return;
        }

        // Must be looking at a block.
        if (mc.hitResult == null || mc.hitResult.getType() != RayTraceResult.Type.BLOCK) {
            placeTimer = 0;
            return;
        }

        // Must be holding a block item in main or off hand.
        ItemStack mainHand = player.getItemInHand(Hand.MAIN_HAND);
        ItemStack offHand  = player.getItemInHand(Hand.OFF_HAND);
        boolean hasBlock = (mainHand.getItem() instanceof BlockItem)
                        || (offHand.getItem() instanceof BlockItem);
        if (!hasBlock) {
            placeTimer = 0;
            return;
        }

        // Clamp interval: 1 (fastest) .. 20.
        int interval = Math.max(1, Math.round(state.settings(FeatureType.FAST_PLACE).speed));

        if (placeTimer > 0) {
            placeTimer--;
            return;
        }

        // Zero vanilla cooldown so the game won't reject our click.
        mc.rightClickDelay = 0;

        // Fire a right-click use on the targeted block.
        BlockRayTraceResult blockHit = (BlockRayTraceResult) mc.hitResult;
        PlayerController pc = mc.gameMode;
        if (pc == null) return;

        // Try main hand first, then off hand (mirrors vanilla logic).
        ActionResultType result = pc.useItemOn(player, mc.level, Hand.MAIN_HAND, blockHit);
        if (result != ActionResultType.SUCCESS && mainHand.getItem() instanceof BlockItem == false) {
            pc.useItemOn(player, mc.level, Hand.OFF_HAND, blockHit);
        }

        placeTimer = interval - 1;
    }
}

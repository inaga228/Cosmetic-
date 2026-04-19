package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;

import java.lang.reflect.Field;

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

    // Fast Place
    private int placeTimer = 0;

    /**
     * Reflected reference to Minecraft.rightClickDelay.
     * In 1.16.5 official Mojang mappings this field is private, so we access
     * it via reflection once and cache the Field object for performance.
     *
     * Possible field names depending on mapping layer:
     *   official / Mojang : "rightClickDelay"
     *   srg / MCP         : "field_71467_aa" (srg name as fallback)
     */
    private static final Field RIGHT_CLICK_DELAY_FIELD;
    static {
        Field f = null;
        for (String name : new String[]{ "rightClickDelay", "field_71467_aa" }) {
            try {
                f = Minecraft.class.getDeclaredField(name);
                f.setAccessible(true);
                break;
            } catch (NoSuchFieldException ignored) {}
        }
        RIGHT_CLICK_DELAY_FIELD = f; // null if neither name matched (shouldn't happen)
    }

    /** Sets Minecraft.rightClickDelay to 0 via reflection. */
    private static void resetRightClickDelay(Minecraft mc) {
        if (RIGHT_CLICK_DELAY_FIELD == null) return;
        try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, 0);
        } catch (IllegalAccessException ignored) {}
    }

    // Fullbright: remember original gamma so we can restore it on disable.
    private float originalGamma = -1F;
    private boolean fullbrightActive = false;

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
    // Zeroes rightClickDelay every tick while RMB is held + block item in hand.
    // Vanilla placement fires immediately each frame without the 4-tick delay.
    // -------------------------------------------------------------------------
    private void tickFastPlace(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.FAST_PLACE)) return;

        if (!mc.options.keyUse.isDown()) {
            placeTimer = 0;
            return;
        }

        // Must be looking at a block.
        if (mc.hitResult == null || mc.hitResult.getType() != RayTraceResult.Type.BLOCK) {
            placeTimer = 0;
            return;
        }

        ItemStack mainHand = player.getItemInHand(Hand.MAIN_HAND);
        ItemStack offHand  = player.getItemInHand(Hand.OFF_HAND);
        boolean mainIsBlock = mainHand.getItem() instanceof BlockItem;
        boolean offIsBlock  = offHand.getItem()  instanceof BlockItem;
        if (!mainIsBlock && !offIsBlock) {
            placeTimer = 0;
            return;
        }

        // Zero the placement cooldown — vanilla handles the actual placement.
        // rightClickDelay is private in 1.16.5 official mappings, so we use reflection.
        resetRightClickDelay(mc);
    }
}

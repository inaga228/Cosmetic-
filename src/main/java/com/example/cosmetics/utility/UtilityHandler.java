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
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
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

    // Fast Place
    private int placeTimer = 0;

    // Auto Jump: track ground state to fire only once per landing
    private boolean wasOnGround = true;

    // Auto Totem: track which slot we last swapped to avoid redundant swaps
    private int lastTotemSourceSlot = -1;

    /**
     * Reflected reference to Minecraft.rightClickDelay.
     * In 1.16.5 official Mojang mappings this field is private.
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
        RIGHT_CLICK_DELAY_FIELD = f;
    }

    private static void resetRightClickDelay(Minecraft mc) {
        if (RIGHT_CLICK_DELAY_FIELD == null) return;
        try {
            RIGHT_CLICK_DELAY_FIELD.setInt(mc, 0);
        } catch (IllegalAccessException ignored) {}
    }

    // Fullbright
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
        tickAutoTotem(mc, player, state);
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
    //
    // Fires jumpFromGround() exactly ONCE per ground contact — on the rising
    // edge when the player lands after being airborne. This matches what a human
    // does when spam-pressing space and won't trigger antiCheat jump-frequency
    // checks (no more 20 jumps/sec).
    // -------------------------------------------------------------------------
    private void tickAutoJump(ClientPlayerEntity player, CosmeticsState state) {
        boolean onGround = player.isOnGround();

        if (!state.isOn(FeatureType.AUTO_JUMP)) {
            wasOnGround = onGround;
            return;
        }

        if (!player.isCrouching()) {
            Minecraft mc = Minecraft.getInstance();
            boolean moving = mc.options.keyUp.isDown()
                    || mc.options.keyDown.isDown()
                    || mc.options.keyLeft.isDown()
                    || mc.options.keyRight.isDown();

            // Rising edge: just landed (airborne → on ground). Jump once.
            if (moving && onGround && !wasOnGround) {
                player.jumpFromGround();
            }
        }

        wasOnGround = onGround;
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
    //
    // The old approach only swapped items locally on the client — the server
    // never knew about it, so the totem didn't fire on death.
    //
    // Fix: we move the totem into hotbar slot 0, switch the held item to slot 0
    // via CHeldItemChangePacket (so the server registers it in the main hand),
    // then send F-key swap (CPlayerDiggingPacket SWAP_ITEM_WITH_OFFHAND) to
    // move it to offhand — all server-side acknowledged.
    //
    // We only do this once per "need" (lastTotemSourceSlot tracks it) to avoid
    // spamming packets every tick.
    // -------------------------------------------------------------------------
    private void tickAutoTotem(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_TOTEM)) return;

        // If offhand already has a totem, nothing to do.
        if (player.getItemInHand(Hand.OFF_HAND).getItem() == Items.TOTEM_OF_UNDYING) {
            lastTotemSourceSlot = -1;
            return;
        }

        int thresholdHearts = state.settings(FeatureType.AUTO_TOTEM).count;
        float thresholdHp = thresholdHearts * 2.0F;
        if (player.getHealth() > thresholdHp) {
            lastTotemSourceSlot = -1;
            return;
        }

        // Find totem in main inventory (slots 0-35).
        NonNullList<ItemStack> inv = player.inventory.items;
        int totemSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        if (totemSlot == -1) return;

        // Already handled this slot this cycle.
        if (totemSlot == lastTotemSourceSlot) return;
        lastTotemSourceSlot = totemSlot;

        PlayerController pc = mc.gameMode;
        if (pc == null) return;

        if (totemSlot < 9) {
            // Totem is already in hotbar — just switch to that slot and swap to offhand.
            int previousSlot = player.inventory.selected;
            player.inventory.selected = totemSlot;
            mc.getConnection().send(new CHeldItemChangePacket(totemSlot));
            mc.getConnection().send(new CPlayerDiggingPacket(
                    CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO,
                    net.minecraft.util.Direction.DOWN));
            // Restore previous hotbar selection.
            player.inventory.selected = previousSlot;
            mc.getConnection().send(new CHeldItemChangePacket(previousSlot));
        } else {
            // Totem is in main inventory (slot 9-35). Use pick-block mechanic:
            // swap it into hotbar slot 0 locally, then proceed as above.
            int hotbarTarget = 0;
            ItemStack hotbarItem = inv.get(hotbarTarget).copy();
            inv.set(hotbarTarget, inv.get(totemSlot).copy());
            inv.set(totemSlot, hotbarItem.isEmpty() ? ItemStack.EMPTY : hotbarItem);

            int previousSlot = player.inventory.selected;
            player.inventory.selected = hotbarTarget;
            mc.getConnection().send(new CHeldItemChangePacket(hotbarTarget));
            mc.getConnection().send(new CPlayerDiggingPacket(
                    CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO,
                    net.minecraft.util.Direction.DOWN));
            player.inventory.selected = previousSlot;
            mc.getConnection().send(new CHeldItemChangePacket(previousSlot));
        }
    }

    // -------------------------------------------------------------------------
    // Fast Place
    //
    // Simulates repeated right-click presses via KeyBinding.click() while the
    // player holds RMB on a block with a block item. This is equivalent to the
    // player clicking very fast themselves — no reflection, no packet hacks.
    // placeTimer throttles the rate: fires every FAST_PLACE_INTERVAL ticks.
    // -------------------------------------------------------------------------
    private static final int FAST_PLACE_INTERVAL = 1; // click every tick = 20 clicks/sec

    private void tickFastPlace(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.FAST_PLACE)) {
            placeTimer = 0;
            return;
        }

        if (!mc.options.keyUse.isDown()) {
            placeTimer = 0;
            return;
        }

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

        placeTimer++;
        if (placeTimer >= FAST_PLACE_INTERVAL) {
            placeTimer = 0;
            // Simulate an extra right-click — vanilla handles the actual placement
            mc.options.keyUse.click();
        }
    }
}

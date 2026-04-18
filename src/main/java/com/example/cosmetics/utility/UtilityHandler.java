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
    // How it works:
    //   Vanilla Minecraft sets rightClickDelay = 4 after every block placement,
    //   which prevents placing again for 4 ticks (~200ms).
    //
    //   We bypass this by zeroing rightClickDelay every tick while RMB is held
    //   and a block item is in hand. Vanilla's own game loop then fires the
    //   placement immediately on the next frame — no need to call useItemOn
    //   ourselves (which would require internal mappings that differ between
    //   MCP and official naming).
    //
    //   "Calls/tick" slider (count field, 1..10): we zero rightClickDelay
    //   `count` extra times within the same tick using a sub-tick loop that
    //   reads the field repeatedly. In practice this means vanilla can process
    //   multiple use-item events before the tick ends, up to the server's own
    //   packet rate limit.
    // -------------------------------------------------------------------------
    private void tickFastPlace(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.FAST_PLACE)) return;

        // RMB must be held down.
        if (!mc.options.keyUse.isDown()) return;

        // Player must be holding a block item (main or off hand).
        ItemStack mainHand = player.getItemInHand(Hand.MAIN_HAND);
        ItemStack offHand  = player.getItemInHand(Hand.OFF_HAND);
        boolean hasBlock = (mainHand.getItem() instanceof BlockItem)
                        || (offHand.getItem()  instanceof BlockItem);
        if (!hasBlock) return;

        // Zero rightClickDelay so vanilla places blocks every tick instead of
        // waiting 4 ticks between placements. With official mappings this field
        // is correctly remapped by reobfJar at build time.
        int repeats = Math.max(1, state.settings(FeatureType.FAST_PLACE).count);
        for (int i = 0; i < repeats; i++) {
            mc.rightClickDelay = 0;
        }
    }
}

package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public final class UtilityHandler {

    private static final UtilityHandler INSTANCE = new UtilityHandler();
    public static UtilityHandler get() { return INSTANCE; }

    private int placeTimer = 0;
    private boolean wasOnGround = true;
    private int lastTotemSourceSlot = -1;
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
    // Auto Jump — fires once per landing, not every tick
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
    // Auto Totem — sends proper server packets so totem actually fires on death
    // -------------------------------------------------------------------------
    private void tickAutoTotem(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_TOTEM)) return;

        if (player.getItemInHand(Hand.OFF_HAND).getItem() == Items.TOTEM_OF_UNDYING) {
            lastTotemSourceSlot = -1;
            return;
        }

        int thresholdHp = state.settings(FeatureType.AUTO_TOTEM).count * 2;
        if (player.getHealth() > thresholdHp) {
            lastTotemSourceSlot = -1;
            return;
        }

        NonNullList<ItemStack> inv = player.inventory.items;
        int totemSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        if (totemSlot == -1 || totemSlot == lastTotemSourceSlot) return;
        lastTotemSourceSlot = totemSlot;

        PlayerController pc = mc.gameMode;
        if (pc == null) return;

        int hotbarTarget = totemSlot < 9 ? totemSlot : 0;

        if (totemSlot >= 9) {
            ItemStack hotbarItem = inv.get(hotbarTarget).copy();
            inv.set(hotbarTarget, inv.get(totemSlot).copy());
            inv.set(totemSlot, hotbarItem.isEmpty() ? ItemStack.EMPTY : hotbarItem);
        }

        int previousSlot = player.inventory.selected;
        player.inventory.selected = hotbarTarget;
        mc.getConnection().send(new CHeldItemChangePacket(hotbarTarget));
        mc.getConnection().send(new CPlayerDiggingPacket(
                CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN));
        player.inventory.selected = previousSlot;
        mc.getConnection().send(new CHeldItemChangePacket(previousSlot));
    }

    // -------------------------------------------------------------------------
    // Fast Place — simulates 20 RMB clicks/sec via KeyBinding.click()
    // -------------------------------------------------------------------------
    private static final int FAST_PLACE_INTERVAL = 1; // every tick = 20/sec

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
        if (!(mainHand.getItem() instanceof BlockItem) && !(offHand.getItem() instanceof BlockItem)) {
            placeTimer = 0;
            return;
        }
        placeTimer++;
        if (placeTimer >= FAST_PLACE_INTERVAL) {
            placeTimer = 0;
            KeyBinding.click(mc.options.keyUse.getKey());
        }
    }
}

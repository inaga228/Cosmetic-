package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

public final class UtilityHandler {

    private static final UtilityHandler INSTANCE = new UtilityHandler();
    public static UtilityHandler get() { return INSTANCE; }

    private boolean wasOnGround     = true;
    private int     lastTotemSlot   = -1;
    private float   originalGamma   = -1F;
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
    }

    // Auto Sprint
    private void tickAutoSprint(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_SPRINT)) return;
        if (player.isCrouching() || player.isInWater() || player.isInLava()) return;
        if (player.hasEffect(Effects.BLINDNESS)) return;
        if (Minecraft.getInstance().options.keyUp.isDown()) player.setSprinting(true);
    }

    // Auto Jump
    private void tickAutoJump(ClientPlayerEntity player, CosmeticsState state) {
        boolean onGround = player.isOnGround();
        if (!state.isOn(FeatureType.AUTO_JUMP)) { wasOnGround = onGround; return; }
        if (!player.isCrouching()) {
            Minecraft mc = Minecraft.getInstance();
            boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                          || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
            if (moving && onGround && !wasOnGround) player.jumpFromGround();
        }
        wasOnGround = onGround;
    }

    // Auto Sneak
    private void tickAutoSneak(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_SNEAK)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                      || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
        PlayerAbilities ab = player.abilities;
        if (!moving && !ab.flying) mc.options.keyShift.setDown(true);
    }

    // Fullbright
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

    // Auto Totem
    private void tickAutoTotem(Minecraft mc, ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_TOTEM)) return;
        if (player.getItemInHand(Hand.OFF_HAND).getItem() == Items.TOTEM_OF_UNDYING) {
            lastTotemSlot = -1; return;
        }
        int threshold = state.settings(FeatureType.AUTO_TOTEM).count * 2;
        if (player.getHealth() > threshold) { lastTotemSlot = -1; return; }

        NonNullList<ItemStack> inv = player.inventory.items;
        int slot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.TOTEM_OF_UNDYING) { slot = i; break; }
        }
        if (slot == -1 || slot == lastTotemSlot) return;
        lastTotemSlot = slot;

        PlayerController pc = mc.gameMode;
        if (pc == null) return;
        int hotbar = slot < 9 ? slot : 0;
        if (slot >= 9) {
            ItemStack tmp = inv.get(hotbar).copy();
            inv.set(hotbar, inv.get(slot).copy());
            inv.set(slot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }
        int prev = player.inventory.selected;
        player.inventory.selected = hotbar;
        mc.getConnection().send(new CHeldItemChangePacket(hotbar));
        mc.getConnection().send(new CPlayerDiggingPacket(
                CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        player.inventory.selected = prev;
        mc.getConnection().send(new CHeldItemChangePacket(prev));
    }
}

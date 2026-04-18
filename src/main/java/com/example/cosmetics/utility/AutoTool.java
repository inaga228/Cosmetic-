package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/**
 * Switches the hotbar selection to the best available tool for the block
 * currently being looked at. Only considers hotbar slots 0..8 — we don't
 * rearrange the inventory.
 *
 * Runs every tick while the player is holding LMB on a block. Keeps a
 * "preferred index" so we don't flicker through slots between ticks; we
 * only change slot when the newly computed best is strictly better than
 * the currently selected one.
 */
public final class AutoTool {

    public static void tick() {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_TOOL)) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.level == null) return;
        if (mc.screen != null) return;

        // Only switch while we're actually trying to break a block.
        if (!mc.options.keyAttack.isDown()) return;

        RayTraceResult hit = mc.hitResult;
        if (!(hit instanceof BlockRayTraceResult)) return;
        BlockRayTraceResult brt = (BlockRayTraceResult) hit;
        BlockState bs = mc.level.getBlockState(brt.getBlockPos());
        if (bs.isAir()) return;

        PlayerInventory inv = p.inventory;
        int bestSlot = inv.selected;
        float bestSpeed = inv.getItem(inv.selected).getDestroySpeed(bs);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            float spd = stack.getDestroySpeed(bs);
            if (spd > bestSpeed + 0.001F) {
                bestSpeed = spd;
                bestSlot = i;
            }
        }

        if (bestSlot != inv.selected) {
            inv.selected = bestSlot;
        }
    }

    private AutoTool() {}
}

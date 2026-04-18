package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
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
 *  - Auto Totem (moves a totem to offhand when HP <= threshold)
 *
 * Called once per client tick from ClientEvents.
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
            if (originalGamma >= 0) {
                mc.options.gamma = originalGamma;
            }
            fullbrightActive = false;
        }
    }

    // -------------------------------------------------------------------------
    // Auto Totem
    // Если HP игрока <= порогу (в очках здоровья, т.е. count * 2 = поинты),
    // ищем тотем бессмертия в инвентаре и перемещаем его в оффхенд.
    // -------------------------------------------------------------------------
    private void tickAutoTotem(ClientPlayerEntity player, CosmeticsState state) {
        if (!state.isOn(FeatureType.AUTO_TOTEM)) return;

        // count хранится как число «сердец» (1 сердце = 2 HP points).
        // По умолчанию 6 = 6 сердец = 12 HP.
        int thresholdHearts = state.settings(FeatureType.AUTO_TOTEM).count;
        float thresholdHp = thresholdHearts * 2.0F;

        float currentHp = player.getHealth();

        // Тотем уже в оффхенде — ничего делать не нужно.
        ItemStack offhand = player.getItemInHand(Hand.OFF_HAND);
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;

        // Проверяем порог HP.
        if (currentHp > thresholdHp) return;

        // Ищем тотем в основном инвентаре (хотбар + основной инвентарь).
        NonNullList<ItemStack> inv = player.inventory.items;
        int totemSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        if (totemSlot == -1) return; // тотема нет в инвентаре

        // Swap: тотем из инвентаря → оффхенд, оффхенд → в тот же слот инвентаря.
        ItemStack totem = inv.get(totemSlot);
        ItemStack currentOffhand = player.getItemInHand(Hand.OFF_HAND).copy();

        // Устанавливаем тотем в оффхенд.
        player.inventory.offhand.set(0, totem.copy());

        // Кладём то, что было в оффхенде, обратно в освободившийся слот.
        if (currentOffhand.isEmpty()) {
            inv.set(totemSlot, ItemStack.EMPTY);
        } else {
            inv.set(totemSlot, currentOffhand);
        }
    }
}

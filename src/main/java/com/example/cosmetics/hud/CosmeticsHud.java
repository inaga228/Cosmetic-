package com.example.cosmetics.hud;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.GuiDraw;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class CosmeticsHud {
    private static float shownAlpha = 0.0F;
    private static long lastTick = 0;

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CosmeticsState s = CosmeticsState.get();
        boolean want = s.isOn(FeatureType.COSMETICS_HUD);
        float target = want ? 1.0F : 0.0F;
        shownAlpha += (target - shownAlpha) * 0.12F;
        if (shownAlpha < 0.01F) return;

        PlayerEntity p = mc.player;
        FontRenderer f = mc.font;

        List<String> lines = new ArrayList<>();
        for (FeatureType ft : s.active()) {
            if (ft == FeatureType.COSMETICS_HUD) continue;
            if (ft == FeatureType.TARGET_HUD) continue;
            lines.add(ft.displayName);
        }

        int padding = 7;
        int lineH = 11;
        String playerName = p.getGameProfile().getName();
        int w = Math.max(130, f.width(playerName) + 44);
        for (String l : lines) w = Math.max(w, f.width("• " + l) + padding * 2 + 6);
        int h = padding + 12 + 5 + lines.size() * lineH + padding;
        if (lines.isEmpty()) h = padding + 12 + 5 + lineH + padding;

        int x = 8, y = 8;
        GuiDraw.roundedPanel(ms, x, y, w, h, shownAlpha);

        int a = Math.max(0, Math.min(255, (int)(shownAlpha * 255)));
        int nameCol = (a << 24) | 0xFFFFFF;
        int dotCol  = (a << 24) | 0x9B6DFF;
        int lineCol = (a << 24) | 0xCCBBFF;
        int divCol  = (Math.max(0, Math.min(255, (int)(shownAlpha * 100))) << 24) | 0x8A5CFF;

        f.drawShadow(ms, playerName, x + padding, y + padding, nameCol);
        AbstractGui.fill(ms, x + padding, y + padding + 11, x + w - padding, y + padding + 12, divCol);

        int ly = y + padding + 15;
        if (lines.isEmpty()) {
            f.drawShadow(ms, "No effects active", x + padding, ly, (a << 24) | 0x777777);
        } else {
            for (String l : lines) {
                f.drawShadow(ms, "•", x + padding, ly, dotCol);
                f.drawShadow(ms, l, x + padding + 8, ly, lineCol);
                ly += lineH;
            }
        }
    }

    private CosmeticsHud() {}
}

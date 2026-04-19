package com.example.cosmetics.gui;

import com.example.cosmetics.config.ConfigManager;
import com.example.cosmetics.config.HudPositionManager;
import com.example.cosmetics.hud.CosmeticsHud;
import com.example.cosmetics.hud.TargetHud;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

/**
 * HUD editor screen: shows all draggable HUD elements with
 * semi-transparent handles so the player can drag them anywhere.
 *
 * Open it from MainMenuScreen → gear icon or a keybind.
 */
public class HudEditScreen extends Screen {

    private static final int HANDLE_ALPHA = 0xAA;

    // Which element is being dragged
    private static final int NONE = -1, COSM = 0, TARGET = 1;
    private int dragging = NONE;
    private double dragOffX, dragOffY;

    // Preview sizes (match HUD actual sizes roughly)
    private static final int COSM_W = 140, COSM_H = 80;
    private static final int TARG_W = 160, TARG_H = 44;

    public HudEditScreen() { super(new StringTextComponent("Edit HUD")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        // Dark translucent overlay
        fill(ms, 0, 0, this.width, this.height, 0x88000000);

        // Instruction text
        drawCenteredString(ms, font, "§eDrag HUD elements to reposition them",
                this.width / 2, 10, 0xFFFFFFFF);
        drawCenteredString(ms, font, "§7Press ESC or ENTER to save and close",
                this.width / 2, 22, 0xAAAAAA);

        HudPositionManager hpm = HudPositionManager.get();

        // Draw Cosmetics HUD handle
        int cx = hpm.getCosmeticsX();
        int cy = hpm.getCosmeticsY();
        drawHandle(ms, cx, cy, COSM_W, COSM_H,
                mouseX, mouseY, "Cosmetics HUD", 0xFF9B6DFF, dragging == COSM);

        // Draw Target HUD handle
        int tx = hpm.getTargetX(TARG_W);
        int ty = hpm.getTargetY();
        drawHandle(ms, tx, ty, TARG_W, TARG_H,
                mouseX, mouseY, "Target HUD", 0xFF4A9BFF, dragging == TARGET);
    }

    private void drawHandle(MatrixStack ms, int x, int y, int w, int h,
                             int mx, int my, String label, int accent, boolean active) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = active ? 0xBB2A2040 : (hover ? 0x882A2040 : 0x661A1430);
        fill(ms, x, y, x + w, y + h, bg);

        // Border
        int borderCol = active ? accent : (hover ? (accent & 0x00FFFFFF | 0xAA000000) : 0x553A3060);
        fill(ms, x,     y,     x + w, y + 1, borderCol);
        fill(ms, x,     y + h - 1, x + w, y + h, borderCol);
        fill(ms, x,     y,     x + 1, y + h, borderCol);
        fill(ms, x + w - 1, y, x + w, y + h, borderCol);

        // Label
        int textCol = active ? (accent | 0xFF000000) : 0xCCFFFFFF;
        drawCenteredString(ms, font, label, x + w / 2, y + h / 2 - 4, textCol);
        if (active) {
            drawCenteredString(ms, font, "§7" + x + ", " + y, x + w / 2, y + h / 2 + 6, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        HudPositionManager hpm = HudPositionManager.get();

        int cx = hpm.getCosmeticsX(), cy = hpm.getCosmeticsY();
        if (mx >= cx && mx <= cx + COSM_W && my >= cy && my <= cy + COSM_H) {
            dragging = COSM; dragOffX = mx - cx; dragOffY = my - cy; return true;
        }

        int tx = hpm.getTargetX(TARG_W), ty = hpm.getTargetY();
        if (mx >= tx && mx <= tx + TARG_W && my >= ty && my <= ty + TARG_H) {
            dragging = TARGET; dragOffX = mx - tx; dragOffY = my - ty; return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0 || dragging == NONE) return false;
        HudPositionManager hpm = HudPositionManager.get();
        int nx = (int)(mx - dragOffX);
        int ny = (int)(my - dragOffY);
        if (dragging == COSM)   hpm.setCosmeticsPos(nx, ny);
        if (dragging == TARGET) hpm.setTargetPos(nx, ny);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) dragging = NONE;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 257) { // ESC or ENTER
            onClose(); return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        ConfigManager.get().save();
        Minecraft.getInstance().setScreen(null);
    }
}

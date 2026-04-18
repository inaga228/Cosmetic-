package com.example.cosmetics.gui;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.widgets.ColorPicker;
import com.example.cosmetics.gui.widgets.Slider;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings panel for a single feature.
 * COLOR features get a visual color picker + RGB sliders.
 * Other caps get appropriately labelled sliders.
 */
public class SettingsScreen extends Screen {

    private final FeatureType feature;
    private final FeatureSettings fs;
    private final List<Slider> sliders = new ArrayList<>();
    private ColorPicker colorPicker;
    private long openedAtMs;

    // Style labels per feature
    private static final String[][] STYLE_LABELS = {
        {"Cone", "Flat", "Wide"},          // CHINA_HAT
        {"Slash", "Stars", "Crit"},        // HIT_EFFECT
    };

    public SettingsScreen(FeatureType feature) {
        super(new StringTextComponent(feature.displayName + " Settings"));
        this.feature = feature;
        this.fs = CosmeticsState.get().settings(feature);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
        sliders.clear();
        colorPicker = null;

        boolean hasColor = feature.has(FeatureType.Caps.COLOR);
        int panelW = hasColor ? 330 : 300;
        int panelH = calcPanelHeight();
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        int sx = px + 12;
        int sy = py + 46;
        int sw = panelW - 24;
        int rowH = 24;
        int i = 0;

        if (hasColor) {
            // Color picker widget (compact)
            colorPicker = new ColorPicker(sx, sy, sw, 50, fs);
            sy += 58;
        }

        if (feature.has(FeatureType.Caps.SIZE)) {
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Size", 0.25F, 3F,
                    () -> fs.size, v -> fs.size = v));
        }
        if (feature.has(FeatureType.Caps.DENSITY)) {
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Density", 0F, 3F,
                    () -> fs.density, v -> fs.density = v));
        }
        if (feature.has(FeatureType.Caps.SPEED)) {
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Speed", 0.25F, 3F,
                    () -> fs.speed, v -> fs.speed = v));
        }
        if (feature.has(FeatureType.Caps.COUNT)) {
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Count", 1F, 30F,
                    () -> (float) fs.count, v -> fs.count = (int) v));
        }
        if (feature.has(FeatureType.Caps.STYLE)) {
            int maxStyle = getStyleMax();
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Style", 0F, maxStyle - 1,
                    () -> (float) fs.style, v -> fs.style = (int) v) {
                @Override
                public String formatValue() {
                    String[] labels = getStyleLabels();
                    int idx = Math.floorMod(fs.style, labels.length);
                    return labels[idx];
                }
            });
        }
        if (feature.has(FeatureType.Caps.OFFSET)) {
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Offset X", -2F, 2F,
                    () -> fs.offsetX, v -> fs.offsetX = v));
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Offset Y", -2F, 2F,
                    () -> fs.offsetY, v -> fs.offsetY = v));
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Offset Z", -2F, 2F,
                    () -> fs.offsetZ, v -> fs.offsetZ = v));
        }
        if (feature.has(FeatureType.Caps.ROTATION)) {
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Rot X", -180F, 180F,
                    () -> fs.rotX, v -> fs.rotX = v));
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Rot Y", -180F, 180F,
                    () -> fs.rotY, v -> fs.rotY = v));
            sliders.add(new Slider(sx, sy + (i++) * rowH, sw, 17, "Rot Z", -180F, 180F,
                    () -> fs.rotZ, v -> fs.rotZ = v));
        }
    }

    private int calcPanelHeight() {
        int rows = 0;
        if (feature.has(FeatureType.Caps.SIZE)) rows++;
        if (feature.has(FeatureType.Caps.DENSITY)) rows++;
        if (feature.has(FeatureType.Caps.SPEED)) rows++;
        if (feature.has(FeatureType.Caps.COUNT)) rows++;
        if (feature.has(FeatureType.Caps.STYLE)) rows++;
        if (feature.has(FeatureType.Caps.OFFSET)) rows += 3;
        if (feature.has(FeatureType.Caps.ROTATION)) rows += 3;
        int colorH = feature.has(FeatureType.Caps.COLOR) ? 58 : 0;
        return 50 + colorH + rows * 24 + 20;
    }

    private String[] getStyleLabels() {
        if (feature == FeatureType.CHINA_HAT) return new String[]{"Cone", "Flat", "Wide"};
        if (feature == FeatureType.HIT_EFFECT) return new String[]{"Slash", "Stars", "Crit"};
        return new String[]{"Style 0", "Style 1", "Style 2"};
    }

    private int getStyleMax() {
        return getStyleLabels().length;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        float anim = animProgress();
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 155) << 24);

        boolean hasColor = feature.has(FeatureType.Caps.COLOR);
        int panelW = hasColor ? 330 : 300;
        int panelH = calcPanelHeight();
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        GuiDraw.roundedPanel(ms, px, py, panelW, panelH, anim);

        // Title bar
        int titleA = Math.max(0, Math.min(255, (int)(anim * 255)));
        fillGradient(ms, px + 2, py + 2, px + panelW - 2, py + 36,
                (titleA << 24) | 0x1A1430, (titleA << 24) | 0x120E22);

        int titleCol = (titleA << 24) | 0xFFFFFF;
        drawCenteredString(ms, this.font, feature.displayName + " Settings",
                px + panelW / 2, py + 14, titleCol);
        int divCol = (Math.max(0, Math.min(255, (int)(anim * 180))) << 24) | 0x8A5CFF;
        fill(ms, px + panelW / 2 - 70, py + 28, px + panelW / 2 + 70, py + 30, divCol);

        if (colorPicker != null) colorPicker.draw(ms, mouseX, mouseY, anim);
        for (Slider s : sliders) s.draw(ms, anim);

        int hintA = Math.max(0, Math.min(255, (int)(anim * 140)));
        drawCenteredString(ms, this.font, "ESC to go back",
                px + panelW / 2, py + panelH - 13, (hintA << 24) | 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (colorPicker != null && colorPicker.mousePressed(mx, my, button)) return true;
        for (Slider s : sliders) if (s.mousePressed(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (colorPicker != null) colorPicker.mouseReleased();
        for (Slider s : sliders) s.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (colorPicker != null) colorPicker.mouseDragged(mx, my);
        for (Slider s : sliders) s.mouseDragged(mx);
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 344) {
            Minecraft.getInstance().setScreen(new MainMenuScreen());
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private float animProgress() {
        float t = Math.min(1F, (System.currentTimeMillis() - openedAtMs) / 200F);
        return 1F - (1F - t) * (1F - t);
    }
}

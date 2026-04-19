package com.example.cosmetics.gui;

import com.example.cosmetics.client.BindManager;
import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.widgets.ColorPicker;
import com.example.cosmetics.gui.widgets.CycleButton;
import com.example.cosmetics.gui.widgets.Slider;
import com.example.cosmetics.gui.widgets.ToggleButton;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {

    private final FeatureType    feature;
    private final FeatureSettings fs;
    private final List<Slider>       sliders = new ArrayList<>();
    private final List<ToggleButton> toggles = new ArrayList<>();
    private final List<CycleButton>  cycles  = new ArrayList<>();
    private ColorPicker colorPicker;
    private long openedAtMs;

    public SettingsScreen(FeatureType feature) {
        super(new StringTextComponent(feature.displayName + " Settings"));
        this.feature = feature;
        this.fs = CosmeticsState.get().settings(feature);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
        sliders.clear(); toggles.clear(); cycles.clear();
        colorPicker = null;

        boolean hasColor = feature.has(FeatureType.Caps.COLOR);
        int panelW = hasColor ? 340 : 310;
        int panelH = calcPanelHeight();
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        int sx = px + 12, sy = py + 46, sw = panelW - 24, rowH = 24;
        int i = 0;

        if (hasColor) { colorPicker = new ColorPicker(sx, sy, sw, 50, fs); sy += 58; }

        // ══════════════════════════════════════════════════════
        // SLIDERS — общие для многих фич
        // ══════════════════════════════════════════════════════

        if (feature.has(FeatureType.Caps.SIZE)) {
            String lbl = feature == FeatureType.SMOOTH_AIM ? "FOV"
                       : feature == FeatureType.KILL_AURA  ? "Range"
                       : "Size";
            float mn = feature == FeatureType.SMOOTH_AIM ? 1F
                     : feature == FeatureType.KILL_AURA  ? 0.5F
                     : 0.25F;
            float mx = feature == FeatureType.SMOOTH_AIM ? 360F
                     : feature == FeatureType.KILL_AURA  ? 10F
                     : 3F;
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, lbl, mn, mx,
                    () -> fs.size, v -> fs.size = v));
        }
        if (feature.has(FeatureType.Caps.DENSITY)) {
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Density", 0F, 3F,
                    () -> fs.density, v -> fs.density = v));
        }
        if (feature.has(FeatureType.Caps.SPEED)) {
            String lbl = feature == FeatureType.SMOOTH_AIM  ? "Smoothness"
                       : feature == FeatureType.AUTO_CLICKER ? "Max CPS"
                       : feature == FeatureType.STRAFE       ? "Speed"
                       : "Speed";
            float mn = feature == FeatureType.SMOOTH_AIM  ? 1F
                     : feature == FeatureType.AUTO_CLICKER ? 1F
                     : feature == FeatureType.STRAFE       ? 0.5F
                     : 0.25F;
            float mx = feature == FeatureType.SMOOTH_AIM  ? 20F
                     : feature == FeatureType.AUTO_CLICKER ? 20F
                     : feature == FeatureType.STRAFE       ? 5F
                     : 3F;
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, lbl, mn, mx,
                    () -> fs.speed, v -> fs.speed = v));
        }
        if (feature.has(FeatureType.Caps.COUNT)) {
            if (feature == FeatureType.AUTO_TOTEM) {
                sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "HP Threshold", 1F, 10F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)) {
                    @Override public String formatValue() { return fs.count + " ♥"; }
                });
            } else if (feature == FeatureType.AUTO_POT || feature == FeatureType.AUTO_GAP) {
                sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "HP Trigger", 1F, 19F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)) {
                    @Override public String formatValue() { return fs.count + " ♥"; }
                });
            } else if (feature == FeatureType.AUTO_CLICKER) {
                sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Min CPS", 1F, 20F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)));
            } else if (feature == FeatureType.CUSTOM_PLACE) {
                sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Speed", 1F, 10F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)) {
                    @Override public String formatValue() {
                        if (fs.count >= 10) return "Max";
                        if (fs.count >= 7)  return "Very Fast";
                        if (fs.count >= 4)  return "Fast";
                        if (fs.count >= 2)  return "Normal";
                        return "Slow";
                    }
                });
            } else {
                sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Count", 1F, 30F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)));
            }
        }
        if (feature.has(FeatureType.Caps.STYLE)) {
            String[] labels = getStyleLabels();
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Style", 0F, labels.length - 1,
                    () -> (float) fs.style, v -> fs.style = Math.round(v)) {
                @Override public String formatValue() {
                    return labels[Math.floorMod(fs.style, labels.length)];
                }
            });
        }
        if (feature.has(FeatureType.Caps.OFFSET)) {
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Offset X", -2F, 2F, () -> fs.offsetX, v -> fs.offsetX = v));
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Offset Y", -2F, 2F, () -> fs.offsetY, v -> fs.offsetY = v));
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Offset Z", -2F, 2F, () -> fs.offsetZ, v -> fs.offsetZ = v));
        }
        if (feature.has(FeatureType.Caps.ROTATION)) {
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Rot X", -180F, 180F, () -> fs.rotX, v -> fs.rotX = v));
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Rot Y", -180F, 180F, () -> fs.rotY, v -> fs.rotY = v));
            sliders.add(new Slider(sx, sy + i++ * rowH, sw, 17, "Rot Z", -180F, 180F, () -> fs.rotZ, v -> fs.rotZ = v));
        }

        // ══════════════════════════════════════════════════════
        // BOOL — галочки и выборы, у каждой фичи своё
        // ══════════════════════════════════════════════════════
        if (feature.has(FeatureType.Caps.BOOL)) {
            addBoolWidgets(sx, sy, sw, rowH, i);
        }
    }

    /** Добавляет виджеты-галочки/выборы для конкретной фичи. */
    private void addBoolWidgets(int sx, int sy, int sw, int rowH, int startI) {
        int i = startI;

        // ---- Trails ----------------------------------------------------------
        if (feature == FeatureType.RAINBOW_TRAIL
         || feature == FeatureType.FLAME_TRAIL
         || feature == FeatureType.GALAXY_TRAIL) {
            cycles.add(new CycleButton(sx, sy + i++ * rowH, sw, 17, "Fade Mode",
                    new String[]{"Solid", "Fade Out", "Rainbow"},
                    () -> fs.trailFadeMode, v -> fs.trailFadeMode = v));
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Only When Moving",
                    () -> fs.trailOnlyWhenMoving, v -> fs.trailOnlyWhenMoving = v));
        }

        // ---- Auras -----------------------------------------------------------
        else if (feature == FeatureType.AURA
              || feature == FeatureType.SNOW_AURA
              || feature == FeatureType.HEART_AURA) {
            cycles.add(new CycleButton(sx, sy + i++ * rowH, sw, 17, "Shape",
                    new String[]{"Orbit", "Random", "Pulse"},
                    () -> fs.auraShape, v -> fs.auraShape = v));
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Rotate",
                    () -> fs.auraRotate, v -> fs.auraRotate = v));
        }

        // ---- Wings -----------------------------------------------------------
        else if (feature == FeatureType.DRAGON_WINGS) {
            toggles.add(new ToggleButton(sx, sy + i++ * rowH, sw, 17, "Flap Animation",
                    () -> fs.wingsFlapAnim, v -> fs.wingsFlapAnim = v));
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Only When Sprinting",
                    () -> fs.wingsOnlySprinting, v -> fs.wingsOnlySprinting = v));
        }

        // ---- Kill Aura -------------------------------------------------------
        else if (feature == FeatureType.KILL_AURA) {
            cycles.add(new CycleButton(sx, sy + i++ * rowH, sw, 17, "Rotation Mode",
                    new String[]{"Body Track", "Full Lock", "Silent"},
                    () -> fs.killAuraRotMode, v -> fs.killAuraRotMode = v));
            cycles.add(new CycleButton(sx, sy + i++ * rowH, sw, 17, "Target Sort",
                    new String[]{"Closest", "Lowest HP", "Highest HP"},
                    () -> fs.killAuraSortMode, v -> fs.killAuraSortMode = v));
            toggles.add(new ToggleButton(sx, sy + i++ * rowH, sw, 17, "Auto Crit",
                    () -> fs.killAuraAutoCrit, v -> fs.killAuraAutoCrit = v));
            toggles.add(new ToggleButton(sx, sy + i++ * rowH, sw, 17, "Target Players",
                    () -> fs.killAuraTargetPlayers, v -> fs.killAuraTargetPlayers = v));
            toggles.add(new ToggleButton(sx, sy + i++ * rowH, sw, 17, "Target Hostile",
                    () -> fs.killAuraTargetHostile, v -> fs.killAuraTargetHostile = v));
            toggles.add(new ToggleButton(sx, sy + i++ * rowH, sw, 17, "Target Passive",
                    () -> fs.killAuraTargetPassive, v -> fs.killAuraTargetPassive = v));
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Anti Bot Filter",
                    () -> fs.killAuraAntiBot, v -> fs.killAuraAntiBot = v));
        }

        // ---- Smooth Aim ------------------------------------------------------
        else if (feature == FeatureType.SMOOTH_AIM) {
            toggles.add(new ToggleButton(sx, sy + i++ * rowH, sw, 17, "Only Players",
                    () -> fs.smoothAimOnlyPlayers, v -> fs.smoothAimOnlyPlayers = v));
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Need Weapon in Hand",
                    () -> fs.smoothAimNeedWeapon, v -> fs.smoothAimNeedWeapon = v));
        }

        // ---- Auto Clicker ----------------------------------------------------
        else if (feature == FeatureType.AUTO_CLICKER) {
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Only Target in Range",
                    () -> fs.clickerOnlyInRange, v -> fs.clickerOnlyInRange = v));
        }

        // ---- Strafe ----------------------------------------------------------
        else if (feature == FeatureType.STRAFE) {
            cycles.add(new CycleButton(sx, sy + i * rowH, sw, 17, "Direction",
                    new String[]{"Left", "Right", "Random"},
                    () -> fs.strafeDirection, v -> fs.strafeDirection = v));
        }

        // ---- HUDs ------------------------------------------------------------
        else if (feature == FeatureType.COSMETICS_HUD || feature == FeatureType.TARGET_HUD) {
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Only When Active",
                    () -> fs.hudOnlyWhenActive, v -> fs.hudOnlyWhenActive = v));
        }

        // ---- Auto Totem ------------------------------------------------------
        else if (feature == FeatureType.AUTO_TOTEM) {
            toggles.add(new ToggleButton(sx, sy + i * rowH, sw, 17, "Show Alert Message",
                    () -> fs.totemShowAlert, v -> fs.totemShowAlert = v));
        }
    }

    // ---- Подсчёт высоты панели -----------------------------------------------
    private int calcPanelHeight() {
        int rows = 0;
        if (feature.has(FeatureType.Caps.SIZE))     rows++;
        if (feature.has(FeatureType.Caps.DENSITY))  rows++;
        if (feature.has(FeatureType.Caps.SPEED))    rows++;
        if (feature.has(FeatureType.Caps.COUNT))    rows++;
        if (feature.has(FeatureType.Caps.STYLE))    rows++;
        if (feature.has(FeatureType.Caps.OFFSET))   rows += 3;
        if (feature.has(FeatureType.Caps.ROTATION)) rows += 3;
        if (feature.has(FeatureType.Caps.BOOL))     rows += boolRowCount();
        int colorH = feature.has(FeatureType.Caps.COLOR) ? 58 : 0;
        return 50 + colorH + rows * 24 + 20;
    }

    private int boolRowCount() {
        if (feature == FeatureType.KILL_AURA)   return 7;
        if (feature == FeatureType.DRAGON_WINGS
         || feature == FeatureType.SMOOTH_AIM)  return 2;
        return 2; // default: 1 cycle + 1 toggle or 2 toggles
    }

    private String[] getStyleLabels() {
        if (feature == FeatureType.CHINA_HAT)     return new String[]{"Cone", "Flat", "Wide"};
        if (feature == FeatureType.HIT_EFFECT)    return com.example.cosmetics.hit.HitEffectHandler.STYLE_NAMES;
        if (feature == FeatureType.COSMETICS_HUD) return com.example.cosmetics.hud.CosmeticsHud.STYLE_NAMES;
        if (feature == FeatureType.TARGET_HUD)    return com.example.cosmetics.hud.TargetHud.STYLE_NAMES;
        if (feature == FeatureType.DRAGON_WINGS)  return com.example.cosmetics.render.WingsRenderer.STYLE_NAMES;
        if (feature == FeatureType.JUMP_CIRCLES
         || feature == FeatureType.LANDING_RING)  return com.example.cosmetics.effects.JumpCircles.STYLE_NAMES;
        if (feature == FeatureType.RAINBOW_TRAIL
         || feature == FeatureType.FLAME_TRAIL
         || feature == FeatureType.GALAXY_TRAIL)  return new String[]{"Ribbon", "Blade", "Double"};
        return new String[]{"Style 0", "Style 1", "Style 2"};
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RENDER
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        float anim = animProgress();
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 155) << 24);

        boolean hasColor = feature.has(FeatureType.Caps.COLOR);
        int panelW = hasColor ? 340 : 310;
        int panelH = calcPanelHeight();
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        GuiDraw.roundedPanel(ms, px, py, panelW, panelH, anim);

        int a = Math.max(0, Math.min(255, (int)(anim * 255)));
        fillGradient(ms, px + 2, py + 2, px + panelW - 2, py + 36,
                (a << 24) | 0x1A1430, (a << 24) | 0x120E22);
        drawCenteredString(ms, this.font, feature.displayName + " Settings",
                px + panelW / 2, py + 14, (a << 24) | 0xFFFFFF);
        int div = (Math.max(0, Math.min(255, (int)(anim * 180))) << 24) | 0x8A5CFF;
        fill(ms, px + panelW / 2 - 70, py + 28, px + panelW / 2 + 70, py + 30, div);

        if (colorPicker != null) colorPicker.draw(ms, mouseX, mouseY, anim);
        for (Slider s : sliders)       s.draw(ms, anim);
        for (ToggleButton t : toggles) t.draw(ms, anim);
        for (CycleButton c : cycles)   c.draw(ms, anim);

        int hintA = Math.max(0, Math.min(255, (int)(anim * 140)));

        // Кнопка бинда
        int bx = px + 10, by = py + panelH - 22, bw = 80, bh = 14;
        int bindKey = BindManager.get().getBind(feature);
        String bindLabel = "Bind: " + BindManager.keyName(bindKey);
        net.minecraft.client.gui.AbstractGui.fill(ms, bx, by, bx + bw, by + bh,
                (hintA << 24) | 0x2A1E5A);
        net.minecraft.client.gui.AbstractGui.fill(ms, bx, by, bx + bw, by + 1,
                (hintA << 24) | 0x7040CC);
        net.minecraft.client.gui.AbstractGui.fill(ms, bx, by + bh - 1, bx + bw, by + bh,
                (hintA << 24) | 0x7040CC);
        drawCenteredString(ms, this.font, bindLabel,
                bx + bw / 2, by + (bh - 8) / 2, (hintA << 24) | 0xC0A8FF);

        drawCenteredString(ms, this.font, "ESC to go back",
                px + panelW / 2 + 20, py + panelH - 13, (hintA << 24) | 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (colorPicker != null && colorPicker.mousePressed(mx, my, button)) return true;
        for (Slider s : sliders)       if (s.mousePressed(mx, my, button)) return true;
        for (ToggleButton t : toggles) if (t.mousePressed(mx, my, button)) return true;
        for (CycleButton c : cycles)   if (c.mousePressed(mx, my, button)) return true;

        // Клик по кнопке бинда
        if (button == 0) {
            boolean hasColor = feature.has(FeatureType.Caps.COLOR);
            int panelW = hasColor ? 340 : 310;
            int panelH = calcPanelHeight();
            int px = (this.width - panelW) / 2;
            int py = (this.height - panelH) / 2;
            int bx = px + 10, by = py + panelH - 22, bw = 80, bh = 14;
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                Minecraft.getInstance().setScreen(new BindKeyScreen(feature, this));
                return true;
            }
        }
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

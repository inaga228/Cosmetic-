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

    // Scroll
    private float scrollOffset = 0F;
    private float scrollTarget = 0F;
    private int   contentHeight = 0;
    private int   viewHeight    = 0;  // видимая зона контента
    private boolean draggingScroll = false;
    private double  dragStartY;
    private float   dragStartOff;

    private static final int SCROLL_W = 6;
    private static final int ROW_H    = 24;
    private static final int MAX_PANEL_H = 280; // максимальная высота панели

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
        scrollOffset = 0F; scrollTarget = 0F;

        boolean hasColor = feature.has(FeatureType.Caps.COLOR);
        int panelW = hasColor ? 340 : 310;

        // Виджеты строятся с y=0 (потом сдвигаем при рендере через scroll)
        int sx = 0, sy = 0, sw = panelW - 24 - SCROLL_W - 4;
        int i = 0;

        if (hasColor) { colorPicker = new ColorPicker(sx, sy, sw, 50, fs); sy += 58; }

        // ── SLIDERS ────────────────────────────────────────────────────────────
        if (feature.has(FeatureType.Caps.SIZE)) {
            String lbl = feature == FeatureType.SMOOTH_AIM ? "FOV"
                       : feature == FeatureType.KILL_AURA  ? "Max Range"
                       : "Size";
            float mn = feature == FeatureType.SMOOTH_AIM ? 1F
                     : feature == FeatureType.KILL_AURA  ? 0.5F : 0.25F;
            float mx = feature == FeatureType.SMOOTH_AIM ? 360F
                     : feature == FeatureType.KILL_AURA  ? 10F  : 3F;
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, lbl, mn, mx,
                    () -> fs.size, v -> fs.size = v));
        }
        if (feature.has(FeatureType.Caps.DENSITY)) {
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Density", 0F, 3F,
                    () -> fs.density, v -> fs.density = v));
        }
        if (feature.has(FeatureType.Caps.SPEED)) {
            String lbl = feature == FeatureType.SMOOTH_AIM   ? "Smoothness"
                       : feature == FeatureType.AUTO_CLICKER  ? "Max CPS"
                       : feature == FeatureType.STRAFE        ? "Speed"
                       : "Speed";
            float mn = feature == FeatureType.SMOOTH_AIM   ? 1F
                     : feature == FeatureType.AUTO_CLICKER  ? 1F
                     : feature == FeatureType.STRAFE        ? 0.5F : 0.25F;
            float mx = feature == FeatureType.SMOOTH_AIM   ? 20F
                     : feature == FeatureType.AUTO_CLICKER  ? 20F
                     : feature == FeatureType.STRAFE        ? 5F   : 3F;
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, lbl, mn, mx,
                    () -> fs.speed, v -> fs.speed = v));
        }
        if (feature.has(FeatureType.Caps.COUNT)) {
            if (feature == FeatureType.AUTO_TOTEM) {
                sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "HP Threshold", 1F, 10F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)) {
                    @Override public String formatValue() { return fs.count + " ♥"; }
                });
            } else if (feature == FeatureType.AUTO_POT || feature == FeatureType.AUTO_GAP) {
                sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "HP Trigger", 1F, 19F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)) {
                    @Override public String formatValue() { return fs.count + " ♥"; }
                });
            } else if (feature == FeatureType.AUTO_CLICKER) {
                sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Min CPS", 1F, 20F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)));
            } else if (feature == FeatureType.CUSTOM_PLACE) {
                sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Speed", 1F, 10F,
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
                sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Count", 1F, 30F,
                        () -> (float) fs.count, v -> fs.count = Math.round(v)));
            }
        }
        if (feature.has(FeatureType.Caps.STYLE)) {
            String[] labels = getStyleLabels();
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Style", 0F, labels.length - 1,
                    () -> (float) fs.style, v -> fs.style = Math.round(v)) {
                @Override public String formatValue() {
                    return labels[Math.floorMod(fs.style, labels.length)];
                }
            });
        }
        if (feature.has(FeatureType.Caps.OFFSET)) {
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Offset X", -2F, 2F, () -> fs.offsetX, v -> fs.offsetX = v));
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Offset Y", -2F, 2F, () -> fs.offsetY, v -> fs.offsetY = v));
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Offset Z", -2F, 2F, () -> fs.offsetZ, v -> fs.offsetZ = v));
        }
        if (feature.has(FeatureType.Caps.ROTATION)) {
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Rot X", -180F, 180F, () -> fs.rotX, v -> fs.rotX = v));
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Rot Y", -180F, 180F, () -> fs.rotY, v -> fs.rotY = v));
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Rot Z", -180F, 180F, () -> fs.rotZ, v -> fs.rotZ = v));
        }

        // ── BOOL / EXTENDED ───────────────────────────────────────────────────
        int ii = i; // финальная копия для лямбды не нужна — используем список напрямую
        addBoolWidgets(sx, sy, sw, ii);

        // Считаем общую высоту контента
        int totalRows = sliders.size() + toggles.size() + cycles.size();
        int colorH = hasColor ? 58 : 0;
        contentHeight = colorH + totalRows * ROW_H + 8;
    }

    private void addBoolWidgets(int sx, int sy, int sw, int startI) {
        int i = startI;

        // ---- Trails ----------------------------------------------------------
        if (feature == FeatureType.RAINBOW_TRAIL
         || feature == FeatureType.FLAME_TRAIL
         || feature == FeatureType.GALAXY_TRAIL) {
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Fade Mode",
                    new String[]{"Solid", "Fade Out", "Rainbow"},
                    () -> fs.trailFadeMode, v -> fs.trailFadeMode = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Only When Moving",
                    () -> fs.trailOnlyWhenMoving, v -> fs.trailOnlyWhenMoving = v));
        }

        // ---- Auras -----------------------------------------------------------
        else if (feature == FeatureType.AURA
              || feature == FeatureType.SNOW_AURA
              || feature == FeatureType.HEART_AURA) {
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Shape",
                    new String[]{"Orbit", "Random", "Pulse"},
                    () -> fs.auraShape, v -> fs.auraShape = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Rotate",
                    () -> fs.auraRotate, v -> fs.auraRotate = v));
        }

        // ---- Wings -----------------------------------------------------------
        else if (feature == FeatureType.DRAGON_WINGS) {
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Flap Animation",
                    () -> fs.wingsFlapAnim, v -> fs.wingsFlapAnim = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Only When Sprinting",
                    () -> fs.wingsOnlySprinting, v -> fs.wingsOnlySprinting = v));
        }

        // ---- Kill Aura -------------------------------------------------------
        else if (feature == FeatureType.KILL_AURA) {
            // Rotation & targeting
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Rotation Mode",
                    new String[]{"Body Track", "Full Lock", "Silent"},
                    () -> fs.killAuraRotMode, v -> fs.killAuraRotMode = v));
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Sort",
                    new String[]{"Closest", "Lowest HP", "Highest HP"},
                    () -> fs.killAuraSortMode, v -> fs.killAuraSortMode = v));
            // Range
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Min Range", 0F, 4F,
                    () -> fs.killAuraMinRange, v -> fs.killAuraMinRange = v));
            // FOV
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "FOV", 10F, 360F,
                    () -> fs.killAuraFov, v -> fs.killAuraFov = v) {
                @Override public String formatValue() {
                    return fs.killAuraFov >= 359F ? "360° (any)" : String.format("%.0f°", fs.killAuraFov);
                }
            });
            // Attack delay
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Attack Delay ms", 0F, 500F,
                    () -> (float) fs.killAuraAttackDelay, v -> fs.killAuraAttackDelay = Math.round(v)) {
                @Override public String formatValue() {
                    return fs.killAuraAttackDelay == 0 ? "Off" : fs.killAuraAttackDelay + " ms";
                }
            });
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Delay Rand ±ms", 0F, 200F,
                    () -> fs.killAuraAttackDelayRand, v -> fs.killAuraAttackDelayRand = v) {
                @Override public String formatValue() {
                    return fs.killAuraAttackDelayRand < 1F ? "Off" : String.format("±%.0f ms", fs.killAuraAttackDelayRand);
                }
            });
            // Toggles
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Auto Crit",
                    () -> fs.killAuraAutoCrit, v -> fs.killAuraAutoCrit = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Players",
                    () -> fs.killAuraTargetPlayers, v -> fs.killAuraTargetPlayers = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Hostile",
                    () -> fs.killAuraTargetHostile, v -> fs.killAuraTargetHostile = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Passive",
                    () -> fs.killAuraTargetPassive, v -> fs.killAuraTargetPassive = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Anti Bot Filter",
                    () -> fs.killAuraAntiBot, v -> fs.killAuraAntiBot = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Raytrace (no walls)",
                    () -> fs.killAuraRaytrace, v -> fs.killAuraRaytrace = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Swing Animation",
                    () -> fs.killAuraSwing, v -> fs.killAuraSwing = v));
        }

        // ---- Strafe ----------------------------------------------------------
        else if (feature == FeatureType.STRAFE) {
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Mode",
                    new String[]{"Circle", "Side", "Zigzag"},
                    () -> fs.strafeMode, v -> fs.strafeMode = v));
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Direction",
                    new String[]{"Left", "Right", "Random"},
                    () -> fs.strafeDirection, v -> fs.strafeDirection = v));
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Radius", 1.5F, 6F,
                    () -> fs.strafeRadius, v -> fs.strafeRadius = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Sprint",
                    () -> fs.strafeSprint, v -> fs.strafeSprint = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Only In Combat",
                    () -> fs.strafeOnlyInCombat, v -> fs.strafeOnlyInCombat = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Jitter (anti-predict)",
                    () -> fs.strafeJitter, v -> fs.strafeJitter = v));
        }

        // ---- Smooth Aim ------------------------------------------------------
        else if (feature == FeatureType.SMOOTH_AIM) {
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Only Players",
                    () -> fs.smoothAimOnlyPlayers, v -> fs.smoothAimOnlyPlayers = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Need Weapon in Hand",
                    () -> fs.smoothAimNeedWeapon, v -> fs.smoothAimNeedWeapon = v));
        }

        // ---- Auto Clicker ----------------------------------------------------
        else if (feature == FeatureType.AUTO_CLICKER) {
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Only Target in Range",
                    () -> fs.clickerOnlyInRange, v -> fs.clickerOnlyInRange = v));
        }

        // ---- HUDs ------------------------------------------------------------
        else if (feature == FeatureType.COSMETICS_HUD || feature == FeatureType.TARGET_HUD) {
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Only When Active",
                    () -> fs.hudOnlyWhenActive, v -> fs.hudOnlyWhenActive = v));
        }

        // ---- Auto Totem ------------------------------------------------------
        else if (feature == FeatureType.AUTO_TOTEM) {
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Show Alert Message",
                    () -> fs.totemShowAlert, v -> fs.totemShowAlert = v));
        }

        // ---- ESP -------------------------------------------------------------
        else if (feature == FeatureType.ESP) {
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Mode",
                    new String[]{"Box", "Cube", "Corners", "Glow"},
                    () -> fs.espMode, v -> fs.espMode = v));
            cycles.add(new CycleButton(sx, sy + i++ * ROW_H, sw, 17, "Color Mode",
                    new String[]{"Custom", "By HP", "By Type"},
                    () -> fs.espColorMode, v -> fs.espColorMode = v));
            sliders.add(new Slider(sx, sy + i++ * ROW_H, sw, 17, "Max Range", 5F, 256F,
                    () -> fs.size, v -> fs.size = v) {
                @Override public String formatValue() { return String.format("%.0fm", fs.size); }
            });
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Show Name",
                    () -> fs.espShowName, v -> fs.espShowName = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Show Health Bar",
                    () -> fs.espShowHealth, v -> fs.espShowHealth = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Show Distance",
                    () -> fs.espShowDistance, v -> fs.espShowDistance = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Tracer Line",
                    () -> fs.espShowLine, v -> fs.espShowLine = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Skeleton",
                    () -> fs.espSkeleton, v -> fs.espSkeleton = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Players",
                    () -> fs.espTargetPlayers, v -> fs.espTargetPlayers = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Hostile",
                    () -> fs.espTargetHostile, v -> fs.espTargetHostile = v));
            toggles.add(new ToggleButton(sx, sy + i++ * ROW_H, sw, 17, "Target Passive",
                    () -> fs.espTargetPassive, v -> fs.espTargetPassive = v));
        }
    }

    // ── Layout helpers ─────────────────────────────────────────────────────────

    private int panelW() { return feature.has(FeatureType.Caps.COLOR) ? 340 : 310; }

    /** Высота панели — ограничена MAX_PANEL_H, остаток скроллится */
    private int panelH() {
        int natural = 46 + contentHeight + 28; // header + content + footer
        return Math.min(MAX_PANEL_H, Math.max(80, natural));
    }

    private int contentTop() {
        // y внутри панели где начинается зона виджетов
        return 46;
    }

    private float maxScroll() { return Math.max(0F, contentHeight - viewHeight); }

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

    // ── RENDER ─────────────────────────────────────────────────────────────────

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        // Smooth scroll
        scrollOffset += (scrollTarget - scrollOffset) * 0.2F;

        float anim = animProgress();
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 155) << 24);

        int panelW = panelW();
        int panelH = panelH();
        int px = (this.width  - panelW) / 2;
        int py = (this.height - panelH) / 2;

        GuiDraw.roundedPanel(ms, px, py, panelW, panelH, anim);

        int a = Math.max(0, Math.min(255, (int)(anim * 255)));
        fillGradient(ms, px + 2, py + 2, px + panelW - 2, py + 36,
                (a << 24) | 0x1A1430, (a << 24) | 0x120E22);
        drawCenteredString(ms, this.font, feature.displayName + " Settings",
                px + panelW / 2, py + 14, (a << 24) | 0xFFFFFF);
        int div = (Math.max(0, Math.min(255, (int)(anim * 180))) << 24) | 0x8A5CFF;
        fill(ms, px + panelW / 2 - 70, py + 28, px + panelW / 2 + 70, py + 30, div);

        // Зона контента
        int ctY  = py + contentTop();
        int footH = 28;
        viewHeight = panelH - contentTop() - footH;

        int scroll = (int) scrollOffset;
        int widgetOriginX = px + 12;
        int widgetOriginY = ctY - scroll;

        // Рисуем виджеты со смещением
        if (colorPicker != null) colorPicker.drawAt(ms, mouseX, mouseY, anim, widgetOriginX, widgetOriginY);

        for (Slider s : sliders)       s.drawAt(ms, anim, widgetOriginX, widgetOriginY);
        for (ToggleButton t : toggles) t.drawAt(ms, anim, widgetOriginX, widgetOriginY);
        for (CycleButton c : cycles)   c.drawAt(ms, anim, widgetOriginX, widgetOriginY);

        // Скроллбар
        drawScrollbar(ms, px, py, panelW, panelH, anim);

        // Footer
        int hintA = Math.max(0, Math.min(255, (int)(anim * 140)));
        int bx = px + 10, by = py + panelH - 22, bw = 80, bh = 14;
        int bindKey = BindManager.get().getBind(feature);
        String bindLabel = "Bind: " + BindManager.keyName(bindKey);
        net.minecraft.client.gui.AbstractGui.fill(ms, bx, by, bx + bw, by + bh, (hintA << 24) | 0x2A1E5A);
        net.minecraft.client.gui.AbstractGui.fill(ms, bx, by, bx + bw, by + 1, (hintA << 24) | 0x7040CC);
        net.minecraft.client.gui.AbstractGui.fill(ms, bx, by + bh - 1, bx + bw, by + bh, (hintA << 24) | 0x7040CC);
        drawCenteredString(ms, this.font, bindLabel, bx + bw / 2, by + (bh - 8) / 2, (hintA << 24) | 0xC0A8FF);

        if (maxScroll() > 0) {
            drawCenteredString(ms, this.font, "Scroll ↕  |  ESC back",
                    px + panelW / 2 + 20, py + panelH - 13, (hintA << 24) | 0xAAAAAA);
        } else {
            drawCenteredString(ms, this.font, "ESC to go back",
                    px + panelW / 2 + 20, py + panelH - 13, (hintA << 24) | 0xAAAAAA);
        }
    }

    private void drawScrollbar(MatrixStack ms, int px, int py, int panelW, int panelH, float anim) {
        if (maxScroll() <= 0) return;
        int sbX  = px + panelW - SCROLL_W - 4;
        int sbY  = py + contentTop();
        int sbH  = viewHeight;
        fill(ms, sbX, sbY, sbX + SCROLL_W, sbY + sbH, argb(0x1A1730, anim));
        float ratio  = (float) viewHeight / contentHeight;
        int thumbH   = Math.max(14, (int)(sbH * ratio));
        float frac   = maxScroll() > 0 ? scrollOffset / maxScroll() : 0;
        int thumbY   = sbY + (int)((sbH - thumbH) * frac);
        boolean hov  = isOverScrollbar(Minecraft.getInstance().mouseHandler.xpos(),
                                       Minecraft.getInstance().mouseHandler.ypos(), px, py, panelW, panelH);
        int col = (hov || draggingScroll) ? 0xFF9B6DFF : 0xFF604090;
        fill(ms, sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + thumbH, withAlpha(col, anim));
        fill(ms, sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + 2, withAlpha(0xFFB898FF, anim));
    }

    // ── INPUT ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollTarget -= (float)(delta * ROW_H);
        scrollTarget  = Math.max(0, Math.min(maxScroll(), scrollTarget));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int panelW = panelW(); int panelH = panelH();
        int px = (this.width - panelW) / 2, py = (this.height - panelH) / 2;

        // Scrollbar drag
        if (button == 0 && isOverScrollbar(mx, my, px, py, panelW, panelH)) {
            draggingScroll = true; dragStartY = my; dragStartOff = scrollOffset;
            return true;
        }

        int scroll = (int) scrollOffset;
        int ox = px + 12, oy = py + contentTop() - scroll;

        if (colorPicker != null && colorPicker.mousePressedAt(mx, my, button, ox, oy)) return true;
        for (Slider s : sliders)       if (s.mousePressedAt(mx, my, button, ox, oy)) return true;
        for (ToggleButton t : toggles) if (t.mousePressedAt(mx, my, button, ox, oy)) return true;
        for (CycleButton c : cycles)   if (c.mousePressedAt(mx, my, button, ox, oy)) return true;

        // Bind button
        if (button == 0) {
            int bx = px + 10, by = py + panelH - 22, bw = 80, bh = 14;
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                Minecraft.getInstance().setScreen(new BindKeyScreen(feature, this));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScroll && button == 0 && maxScroll() > 0) {
            float ratio = (float) contentHeight / viewHeight;
            scrollTarget = Math.max(0, Math.min(maxScroll(), dragStartOff + (float)(my - dragStartY) * ratio));
            scrollOffset = scrollTarget;
            return true;
        }
        int ox = (this.width - panelW()) / 2 + 12;
        int oy = (this.height - panelH()) / 2 + contentTop() - (int) scrollOffset;
        if (colorPicker != null) colorPicker.mouseDraggedAt(mx, my, ox, oy);
        for (Slider s : sliders) s.mouseDraggedAt(mx, ox, oy);
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) draggingScroll = false;
        if (colorPicker != null) colorPicker.mouseReleased();
        for (Slider s : sliders) s.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 344) {
            Minecraft.getInstance().setScreen(new MainMenuScreen());
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean isOverScrollbar(double mx, double my, int px, int py, int panelW, int panelH) {
        if (maxScroll() <= 0) return false;
        int sbX = px + panelW - SCROLL_W - 4;
        int sbY = py + contentTop();
        return mx >= sbX && mx <= sbX + SCROLL_W && my >= sbY && my <= sbY + viewHeight;
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, (int)((argb >>> 24 & 0xFF) * alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
    private static int argb(int rgb, float alpha) {
        return (Math.max(0, Math.min(255, (int)(alpha * 255))) << 24) | (rgb & 0xFFFFFF);
    }

    private float animProgress() {
        float t = Math.min(1F, (System.currentTimeMillis() - openedAtMs) / 200F);
        return 1F - (1F - t) * (1F - t);
    }
}

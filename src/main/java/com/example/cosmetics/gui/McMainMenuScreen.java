package com.example.cosmetics.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class McMainMenuScreen extends Screen {

    private final List<MenuButton> buttons = new ArrayList<>();
    private long openedAtMs;
    private static final long ANIM_IN_MS = 500L;
    private final List<Particle> particles = new ArrayList<>();
    private static final int PARTICLE_COUNT = 30;
    private final Random rng = new Random();

    private static final String VERSION_TAG = "Cosmetics Mod";
    private static final String[] SLOGANS = {
        "Right Shift to open mod menu",
        "Trails, auras, and more!",
        "Made with love on mobile",
        "Now with 100% more sparkle!",
    };
    private final String slogan;

    // Размеры панели — константы
    private static final int PW = 240;
    private static final int PH = 190;

    public McMainMenuScreen() {
        super(new StringTextComponent(""));
        slogan = SLOGANS[new Random().nextInt(SLOGANS.length)];
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
        buttons.clear();
        particles.clear();

        int cx = this.width / 2;

        // Центр панели — чуть выше середины экрана
        int panelTop  = this.height / 2 - PH / 2 - 10;

        // Заголовок занимает верхние 58px панели, кнопки идут после
        int bw = 200, bh = 20, gap = 4;
        int by = panelTop + 60; // первая кнопка — сразу после заголовка

        buttons.add(new MenuButton(cx - bw/2, by,              bw, bh, "\u2694  Singleplayer",  0, () ->
            Minecraft.getInstance().setScreen(new WorldSelectionScreen(this))));
        buttons.add(new MenuButton(cx - bw/2, by+(bh+gap),     bw, bh, "\u26A1  Multiplayer",   1, () ->
            Minecraft.getInstance().setScreen(new MultiplayerScreen(this))));
        buttons.add(new MenuButton(cx - bw/2, by+(bh+gap)*2,   bw, bh, "\u2726  Cosmetics",     2, () ->
            Minecraft.getInstance().setScreen(new com.example.cosmetics.gui.MainMenuScreen())));
        buttons.add(new MenuButton(cx - bw/2, by+(bh+gap)*3,   bw, bh, "\u2699  Options",       3, () ->
            Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options))));
        buttons.add(new MenuButton(cx - bw/2, by+(bh+gap)*4,   bw, bh, "\u2716  Quit",          4, () ->
            Minecraft.getInstance().stop()));

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(
                rng.nextFloat() * this.width,
                rng.nextFloat() * this.height,
                (rng.nextFloat() - 0.5F) * 0.4F,
                -(rng.nextFloat() * 0.3F + 0.1F),
                rng.nextFloat() * 2F + 0.5F
            ));
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float pt) {
        float anim = animProgress();

        for (Particle p : particles) p.update(this.width, this.height);

        // --- Фон ---
        int a = clamp((int)(anim * 255));
        fillGradient(ms, 0, 0, this.width, this.height,
                (a << 24) | 0x0A0818, (a << 24) | 0x120830);

        // --- Частицы ---
        for (Particle p : particles) {
            int col = (int)(p.brightness * 255);
            int pa  = clamp((int)(anim * 200 * p.alpha));
            int px  = (int) p.x, py = (int) p.y;
            int sz  = p.size > 1.5F ? 2 : 1;
            fill(ms, px, py, px+sz, py+sz, (pa << 24) | (col << 16) | (col/2 << 8) | col);
        }

        // --- Панель ---
        int cx = this.width / 2;
        int panelLeft = cx - PW / 2;
        int panelTop  = this.height / 2 - PH / 2 - 10;

        // Тень
        for (int i = 6; i > 0; i--) {
            int ga = clamp((int)(anim * (6-i) * 8));
            fillGradient(ms, panelLeft-i, panelTop-i, panelLeft+PW+i, panelTop+PH+i,
                    (ga << 24) | 0x4A1E90, (ga << 24) | 0x1A0A40);
        }
        // Основная панель
        int pa = clamp((int)(anim * 210));
        fillGradient(ms, panelLeft, panelTop, panelLeft+PW, panelTop+PH,
                (pa << 24) | 0x12103A, (pa << 24) | 0x0C0A28);
        // Рамки
        int ba = clamp((int)(anim * 255));
        fill(ms, panelLeft,       panelTop,      panelLeft+PW,    panelTop+2,    (ba << 24) | 0x8B4FFF);
        fill(ms, panelLeft,       panelTop+2,    panelLeft+4,     panelTop+PH,   (ba << 24) | 0x5A2AB0);
        fill(ms, panelLeft+PW-4,  panelTop+2,    panelLeft+PW,    panelTop+PH,   (ba << 24) | 0x5A2AB0);
        fill(ms, panelLeft,       panelTop+PH-1, panelLeft+PW,    panelTop+PH,   (ba << 24) | 0x3A1A70);

        // --- Заголовок "MINECRAFT" ---
        int titleA = clamp((int)(anim * 255));
        ms.pushPose();
        ms.translate(cx, panelTop + 16, 0);
        ms.scale(2.2F, 2.2F, 1F);
        String title = "MINECRAFT";
        font.drawShadow(ms, title, -font.width(title)/2f, 0, (titleA << 24) | 0xFFFFFF);
        ms.popPose();

        int subA = clamp((int)(anim * 200));
        ms.pushPose();
        ms.translate(cx, panelTop + 42, 0);
        ms.scale(0.85F, 0.85F, 1F);
        String sub = "Java Edition";
        font.drawShadow(ms, sub, -font.width(sub)/2f, 0, (subA << 24) | 0xC8A0FF);
        ms.popPose();

        // Разделитель
        int la = clamp((int)(anim * 120));
        fill(ms, panelLeft+16, panelTop+54, panelLeft+PW-16, panelTop+56, (la << 24) | 0x7040CC);

        // --- Кнопки ---
        float btnAnim = Math.max(0F, (anim - 0.25F) / 0.75F);
        for (MenuButton b : buttons) b.draw(ms, mouseX, mouseY, btnAnim);

        // --- Футер ---
        int fa = clamp((int)(anim * 170));
        drawCenteredString(ms, font, VERSION_TAG,
                this.width/2, this.height - 20, (fa << 24) | 0x9B6DFF);
        drawCenteredString(ms, font, slogan,
                this.width/2, this.height - 10, (fa << 24) | 0x666666);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (MenuButton b : buttons) {
                if (b.contains(mx, my)) { b.onClick(); return true; }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) return true; // ESC ничего не делает
        return super.keyPressed(key, scan, mods);
    }

    private float animProgress() {
        float t = Math.min(1F, (System.currentTimeMillis() - openedAtMs) / (float)ANIM_IN_MS);
        return 1F - (1F-t)*(1F-t)*(1F-t);
    }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ── Кнопка ─────────────────────────────────────────────────────────────────
    private class MenuButton {
        final int x, y, w, h, index;
        final String label;
        final Runnable action;
        float hov = 0F;

        MenuButton(int x, int y, int w, int h, String label, int index, Runnable action) {
            this.x=x; this.y=y; this.w=w; this.h=h;
            this.label=label; this.index=index; this.action=action;
        }

        boolean contains(double mx, double my) {
            return mx>=x && mx<=x+w && my>=y && my<=y+h;
        }
        void onClick() { action.run(); }

        void draw(MatrixStack ms, int mx, int my, float anim) {
            if (anim <= 0) return;
            float delay = index * 0.08F;
            float la = Math.max(0F, Math.min(1F, (anim - delay) / Math.max(0.01F, 1F - delay)));
            if (la <= 0) return;

            hov += (contains(mx,my) ? 1F : -1F) * 0.15F;
            hov = Math.max(0F, Math.min(1F, hov));

            ms.pushPose();
            ms.translate((1F-la)*-40F, 0, 0);

            int a = clamp((int)(la*255));
            // Фон
            fillGradient(ms, x, y, x+w, y+h,
                    withA(blend(0xFF1A1440, 0xFF2A1E5A, hov), a),
                    withA(blend(0xFF0E0C28, 0xFF1A1240, hov), a));
            // Левая полоска
            fill(ms, x, y, x+3, y+h, (clamp((int)(la*(100+hov*155))) << 24) | 0x8B4FFF);
            // Линия при ховере
            if (hov > 0.05F)
                fill(ms, x+3, y, x+w, y+1, withA(0xFF6030CC, (int)(a*hov*0.7F)));
            // Текст
            font.drawShadow(ms, label, x+14, y+(h-8)/2,
                    withA(blend(0xFFAAAAAA, 0xFFFFFFFF, hov), a));
            // Стрелка
            if (hov > 0.1F)
                font.drawShadow(ms, ">", x+w-14, y+(h-8)/2, withA(0xFFB898FF, (int)(a*hov)));

            ms.popPose();
        }

        private int blend(int c1, int c2, float t) {
            t = Math.max(0,Math.min(1,t));
            int r=(int)(((c1>>16)&0xFF)*(1-t)+((c2>>16)&0xFF)*t);
            int g=(int)(((c1>>8)&0xFF)*(1-t)+((c2>>8)&0xFF)*t);
            int b=(int)((c1&0xFF)*(1-t)+(c2&0xFF)*t);
            return 0xFF000000|(r<<16)|(g<<8)|b;
        }
        private int withA(int rgb, int a) {
            return (clamp(a) << 24) | (rgb & 0x00FFFFFF);
        }
    }

    // ── Частица ────────────────────────────────────────────────────────────────
    private static class Particle {
        float x, y, vx, vy, size, alpha, brightness;
        Particle(float x, float y, float vx, float vy, float size) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.size=size;
            this.alpha = 0.3F + (float)Math.random()*0.5F;
            this.brightness = 0.6F + (float)Math.random()*0.4F;
        }
        void update(int sw, int sh) {
            x+=vx; y+=vy;
            if (y < -5) { y=sh+5; x=(float)(Math.random()*sw); }
            if (x < -5) x=sw+5;
            if (x > sw+5) x=-5;
            alpha += (float)(Math.random()-0.5)*0.02F;
            alpha = Math.max(0.1F, Math.min(0.8F, alpha));
        }
    }
}

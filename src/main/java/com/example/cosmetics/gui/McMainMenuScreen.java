package com.example.cosmetics.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Кастомное главное меню Minecraft с анимированным фоном,
 * частицами, плавными кнопками и красивой панелью.
 */
public class McMainMenuScreen extends Screen {

    // Текстуры
    private static final ResourceLocation PANORAMA_OVERLAY =
            new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
    private static final ResourceLocation MOJANG_LOGO =
            new ResourceLocation("textures/gui/title/mojangstudios.png");
    private static final ResourceLocation MINECRAFT_LOGO =
            new ResourceLocation("textures/gui/title/minecraft.png");

    // Кнопки
    private final List<MenuButton> buttons = new ArrayList<>();

    // Анимация открытия
    private long openedAtMs;
    private static final long ANIM_IN_MS = 600L;

    // Плавающие частицы фона
    private final List<Particle> particles = new ArrayList<>();
    private static final int PARTICLE_COUNT = 35;
    private final Random rng = new Random();

    // Прокрутка панорамы
    private float panoramaAngle = 0F;

    // Версия/слоган
    private static final String VERSION_TAG = "Cosmetics Mod v1.0";
    private static final String[] SLOGANS = {
        "Now with 100% more sparkle!",
        "Right Shift to open menu",
        "Trails and auras await...",
        "Made with ❤ on mobile",
    };
    private final String slogan;

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
        int by = this.height / 2 + 10;
        int bw = 200, bh = 20, gap = 4;

        buttons.add(new MenuButton(cx - bw/2, by,            bw, bh, "⚔  Singleplayer",  0, () -> {
            Minecraft.getInstance().setScreen(new WorldSelectionScreen(this));
        }));
        buttons.add(new MenuButton(cx - bw/2, by + bh+gap,    bw, bh, "⚡  Multiplayer",   1, () -> {
            Minecraft.getInstance().setScreen(new MultiplayerScreen(this));
        }));
        buttons.add(new MenuButton(cx - bw/2, by + (bh+gap)*2, bw, bh, "✦  Cosmetics Menu", 2, () -> {
            Minecraft.getInstance().setScreen(new com.example.cosmetics.gui.MainMenuScreen());
        }));
        buttons.add(new MenuButton(cx - bw/2, by + (bh+gap)*3, bw, bh, "⚙  Options",       3, () -> {
            Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options));
        }));
        buttons.add(new MenuButton(cx - bw/2, by + (bh+gap)*4, bw, bh, "✖  Quit",           4, () -> {
            Minecraft.getInstance().stop();
        }));

        // Генерируем частицы
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(
                rng.nextFloat() * this.width,
                rng.nextFloat() * this.height,
                (rng.nextFloat() - 0.5F) * 0.4F,
                -(rng.nextFloat() * 0.3F + 0.1F),
                rng.nextFloat() * 2.5F + 0.5F,
                rng.nextFloat()
            ));
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float pt) {
        float anim = animProgress();
        long now = System.currentTimeMillis();

        // Обновляем частицы
        for (Particle p : particles) p.update(this.width, this.height);

        // Панорама (тёмный градиент-фон)
        drawBackground(ms, anim);

        // Частицы фона
        drawParticles(ms, anim);

        // Тёмная центральная панель
        drawCenterPanel(ms, anim);

        // Лого Minecraft
        drawMinecraftLogo(ms, anim);

        // Кнопки
        float btnAnim = Math.max(0F, (anim - 0.3F) / 0.7F);
        for (MenuButton b : buttons) {
            b.draw(ms, mouseX, mouseY, btnAnim);
        }

        // Версия и слоган внизу
        int footerAlpha = (int)(anim * 180);
        drawCenteredString(ms, font, VERSION_TAG,
                this.width / 2, this.height - 20,
                (footerAlpha << 24) | 0x9B6DFF);
        drawCenteredString(ms, font, slogan,
                this.width / 2, this.height - 10,
                (footerAlpha << 24) | 0x666666);
    }

    private void drawBackground(MatrixStack ms, float anim) {
        // Тёмный фон с фиолетово-синим градиентом
        int a = (int)(anim * 255);
        fillGradient(ms, 0, 0, this.width, this.height,
                (a << 24) | 0x0A0818,
                (a << 24) | 0x120830);

        // Дополнительный градиент снизу — глубина
        fillGradient(ms, 0, this.height/2, this.width, this.height,
                0x00000000,
                (clamp((int)(anim * 120)) << 24) | 0x050210);
    }

    private void drawParticles(MatrixStack ms, float anim) {
        int a = (int)(anim * 200);
        for (Particle p : particles) {
            int col = (int)(p.brightness * 255);
            int alpha = clamp((int)(a * p.alpha));
            // Рисуем как маленький квадратик
            int px = (int) p.x, py = (int) p.y;
            int sz = p.size > 1.5F ? 2 : 1;
            fill(ms, px, py, px + sz, py + sz,
                    (alpha << 24) | (col << 16) | (col/2 << 8) | col); // фиолетовый оттенок
        }
    }

    private void drawCenterPanel(MatrixStack ms, float anim) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int pw = 260, ph = 180;
        int px = cx - pw/2, py = cy - ph/2 + 20;

        // Тень / глоу под панелью
        for (int i = 8; i > 0; i--) {
            int ga = (int)(anim * (8 - i) * 6);
            fillGradient(ms, px - i, py - i, px + pw + i, py + ph + i,
                    (ga << 24) | 0x4A1E90,
                    (ga << 24) | 0x1A0A40);
        }

        // Основная панель
        int panelA = clamp((int)(anim * 200));
        fillGradient(ms, px, py, px + pw, py + ph,
                (panelA << 24) | 0x12103A,
                (panelA << 24) | 0x0C0A28);

        // Рамка сверху — акцент
        int borderA = clamp((int)(anim * 255));
        fill(ms, px, py, px + pw, py + 2, (borderA << 24) | 0x8B4FFF);
        fill(ms, px, py + 2, px + 4, py + ph, (borderA << 24) | 0x5A2AB0);
        fill(ms, px + pw - 4, py + 2, px + pw, py + ph, (borderA << 24) | 0x5A2AB0);
        fill(ms, px, py + ph - 1, px + pw, py + ph, (borderA << 24) | 0x3A1A70);

        // Горизонтальная линия под логотипом
        int lineA = clamp((int)(anim * 120));
        fill(ms, px + 20, py + 58, px + pw - 20, py + 60, (lineA << 24) | 0x7040CC);
    }

    private void drawMinecraftLogo(MatrixStack ms, float anim) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int pw = 260;
        int panelTop = cy - 90 + 20;

        // Рисуем текст "MINECRAFT" вручную крупным стилем
        // (текстура лого требует сложной работы с UV, используем текст)
        int titleA = clamp((int)(anim * 255));

        // Основной заголовок — большой белый текст с тенью
        ms.pushPose();
        ms.translate(cx, panelTop + 16, 0);
        ms.scale(2.5F, 2.5F, 1F);
        String title = "MINECRAFT";
        int tw = font.width(title);
        font.drawShadow(ms, title, -tw / 2f, 0, (titleA << 24) | 0xFFFFFF);
        ms.popPose();

        // Подзаголовок
        int subA = clamp((int)(anim * 200));
        ms.pushPose();
        ms.translate(cx, panelTop + 44, 0);
        ms.scale(0.9F, 0.9F, 1F);
        String sub = "Java Edition";
        int sw = font.width(sub);
        font.drawShadow(ms, sub, -sw / 2f, 0, (subA << 24) | 0xC8A0FF);
        ms.popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (MenuButton b : buttons) {
                if (b.contains(mx, my)) {
                    b.onClick();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // ESC ничего не делает на главном меню
        if (key == 256) return true;
        return super.keyPressed(key, scan, mods);
    }

    private float animProgress() {
        float t = Math.min(1F, (System.currentTimeMillis() - openedAtMs) / (float) ANIM_IN_MS);
        return easeOut(t);
    }
    private static float easeOut(float t) { return 1F - (1F - t) * (1F - t) * (1F - t); }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ── Кнопка ──────────────────────────────────────────────────────────────
    private class MenuButton {
        final int x, y, w, h;
        final String label;
        final int index;
        final Runnable action;
        float hoverAnim = 0F;

        MenuButton(int x, int y, int w, int h, String label, int index, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.index = index; this.action = action;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void onClick() { action.run(); }

        void draw(MatrixStack ms, int mx, int my, float anim) {
            if (anim <= 0) return;

            // Задержка появления по индексу
            float delay = index * 0.08F;
            float localAnim = Math.max(0F, Math.min(1F, (anim - delay) / (1F - delay)));
            if (localAnim <= 0) return;

            boolean hover = contains(mx, my);
            hoverAnim += (hover ? 1F : -1F) * 0.15F;
            hoverAnim = Math.max(0F, Math.min(1F, hoverAnim));

            // Слайд-ин слева
            ms.pushPose();
            float slideX = (1F - localAnim) * -40F;
            ms.translate(slideX, 0, 0);

            int a = clamp((int)(localAnim * 255));

            // Фон кнопки
            int bgLeft  = blend(0xFF1A1440, 0xFF2A1E5A, hoverAnim);
            int bgRight = blend(0xFF0E0C28, 0xFF1A1240, hoverAnim);
            fillGradient(ms, x, y, x + w, y + h,
                    withA(bgLeft, a), withA(bgRight, a));

            // Левая акцент-полоска
            int accentA = clamp((int)(localAnim * (100 + (int)(hoverAnim * 155))));
            fill(ms, x, y, x + 3, y + h, (accentA << 24) | 0x8B4FFF);

            // Верхняя линия при ховере
            if (hoverAnim > 0.05F) {
                fill(ms, x + 3, y, x + w, y + 1,
                        withA(0xFF6030CC, (int)(a * hoverAnim * 0.7F)));
            }

            // Текст
            int textCol = blend(0xFFAAAAAA, 0xFFFFFFFF, hoverAnim);
            font.drawShadow(ms, label,
                    x + 14, y + (h - 8) / 2,
                    withA(textCol, a));

            // Стрелка вправо при ховере
            if (hoverAnim > 0.1F) {
                int arrowA = withA(0xFFB898FF, (int)(a * hoverAnim));
                font.drawShadow(ms, "›", x + w - 14, y + (h - 8) / 2, arrowA);
            }

            ms.popPose();
        }

        private int blend(int c1, int c2, float t) {
            t = Math.max(0, Math.min(1, t));
            int r = (int)(((c1>>16)&0xFF)*(1-t)+((c2>>16)&0xFF)*t);
            int g = (int)(((c1>>8)&0xFF)*(1-t)+((c2>>8)&0xFF)*t);
            int b = (int)((c1&0xFF)*(1-t)+(c2&0xFF)*t);
            return 0xFF000000|(r<<16)|(g<<8)|b;
        }
        private int withA(int rgb, int a) {
            return (clamp(a) << 24) | (rgb & 0x00FFFFFF);
        }
    }

    // ── Частица фона ────────────────────────────────────────────────────────
    private static class Particle {
        float x, y, vx, vy, size, alpha, brightness;

        Particle(float x, float y, float vx, float vy, float size, float phase) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.size = size;
            this.alpha = 0.3F + (float)Math.random() * 0.5F;
            this.brightness = 0.6F + (float)Math.random() * 0.4F;
        }

        void update(int screenW, int screenH) {
            x += vx; y += vy;
            if (y < -5) { y = screenH + 5; x = (float)(Math.random() * screenW); }
            if (x < -5) x = screenW + 5;
            if (x > screenW + 5) x = -5;
            // Мерцание
            alpha += (float)(Math.random() - 0.5) * 0.02F;
            alpha = Math.max(0.1F, Math.min(0.8F, alpha));
        }
    }
}

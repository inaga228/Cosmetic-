package com.example.cosmetics.gui;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.config.ConfigManager;
import com.example.cosmetics.config.ThemeManager;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen extends Screen {

    private static final String REPO_URL = "https://github.com/inaga228/Cosmetic-mod";

    // Layout
    private static final int PANEL_W  = 320;
    private static final int PANEL_H  = 220;
    private static final int TAB_W    = 96;
    private static final int CARD_H   = 22;
    private static final int CARD_GAP = 4;
    private static final int SCROLL_W = 7;   // scrollbar width

    private FeatureType.Category current = FeatureType.Category.TRAILS;
    private long openedAtMs;
    private boolean closing = false;
    private long closingAtMs;
    private static final long ANIM_MS = 240L;

    // Cards & scroll
    private final List<FeatureType> visibleFeatures = new ArrayList<>();
    private float scrollOffset  = 0F;  // pixels scrolled
    private float scrollTarget  = 0F;
    private int   contentHeight = 0;   // total height of card list

    // Card area bounds (computed in init)
    private int cardX, cardY, cardW, cardAreaH;

    // Scrollbar drag
    private boolean draggingScroll = false;
    private double  dragStartY;
    private float   dragStartOffset;

    // Tabs & github
    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private GitHubButton githubButton;
    private ThemeButton themeButton;
    private HudEditButton hudEditButton;

    // Tab scroll
    private float tabScrollOffset = 0F;
    private float tabScrollTarget = 0F;
    private int   tabAreaH        = 0;
    private int   tabContentH     = 0;
    private static final int TAB_SCROLL_W = 4;

    // Panel origin
    private int px, py;

    public MainMenuScreen() { super(new StringTextComponent("Cosmetics")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
        closing    = false;
        categoryTabs.clear();

        px = (this.width  - PANEL_W) / 2;
        py = (this.height - PANEL_H) / 2;

        int i = 0;
        for (FeatureType.Category c : FeatureType.Category.values()) {
            categoryTabs.add(new CategoryTab(px + 8, py + 44 + i * 24, TAB_W, 20, c));
            i++;
        }

        // Tab scroll area: from header to hint bar
        tabAreaH     = PANEL_H - 44 - 28;
        tabContentH  = FeatureType.Category.values().length * 24 - 4;
        tabScrollOffset = 0F; tabScrollTarget = 0F;

        // Card area: right of tabs, above hint bar, leave room for scrollbar
        cardX     = px + TAB_W + 16;
        cardY     = py + 44;
        cardW     = PANEL_W - TAB_W - 28 - SCROLL_W - 4;
        cardAreaH = PANEL_H - 44 - 28; // 28 = hint bar

        rebuildFeatures();

        githubButton = new GitHubButton(px + PANEL_W - 96, py + PANEL_H - 26, 88, 20);
        themeButton  = new ThemeButton(px + 8, py + PANEL_H - 26, 60, 20);
        hudEditButton = new HudEditButton(px + 72, py + PANEL_H - 26, 60, 20);
    }

    private void rebuildFeatures() {
        visibleFeatures.clear();
        for (FeatureType f : FeatureType.values()) {
            if (f.category == current) visibleFeatures.add(f);
        }
        contentHeight = visibleFeatures.size() * (CARD_H + CARD_GAP) - CARD_GAP;
        scrollTarget  = 0F;
        scrollOffset  = 0F;
    }

    private float maxScroll() {
        return Math.max(0F, contentHeight - cardAreaH);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        // Smooth scroll interpolation (each render frame)
        scrollOffset += (scrollTarget - scrollOffset) * 0.2F;
        tabScrollOffset += (tabScrollTarget - tabScrollOffset) * 0.2F;

        float anim = animProgress();
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 155) << 24);

        ms.pushPose();
        float scale = 0.88F + 0.12F * anim;
        ms.translate(this.width / 2f, this.height / 2f, 0);
        ms.scale(scale, scale, 1.0F);
        ms.translate(-this.width / 2f, -this.height / 2f, 0);

        ThemeManager tm = ThemeManager.get();
        GuiDraw.themedPanel(ms, px, py, PANEL_W, PANEL_H, anim, tm.panelTop(), tm.panelBot(), tm.accent());

        // Title bar
        int titleA = clamp((int)(anim * 255));
        fillGradient(ms, px + 2, py + 2, px + PANEL_W - 2, py + 36,
                (titleA << 24) | tm.panelTop(), (titleA << 24) | tm.panelBot());
        drawCenteredString(ms, this.font, "\u2726 Cosmetics \u2726",
                px + PANEL_W / 2, py + 13, (titleA << 24) | 0xFFFFFF);
        int glowC = (clamp((int)(anim * 200)) << 24) | (tm.accent() & 0xFFFFFF);
        fill(ms, px + PANEL_W / 2 - 55, py + 28, px + PANEL_W / 2 + 55, py + 30, glowC);

        // Category tabs (with scroll)
        int tabScroll = (int) tabScrollOffset;
        for (CategoryTab c : categoryTabs) c.draw(ms, mouseX, mouseY, anim, current, tabScroll);

        // Tab scrollbar
        drawTabScrollbar(ms, anim);

        // ---- Scissored card area ------------------------------------------------
        // Draw cards with clipping
        drawCards(ms, mouseX, mouseY, anim);

        // Scrollbar
        drawScrollbar(ms, anim);

        if (githubButton  != null) githubButton.draw(ms, mouseX, mouseY, anim);
        if (themeButton   != null) themeButton.draw(ms, mouseX, mouseY, anim);
        if (hudEditButton != null) hudEditButton.draw(ms, mouseX, mouseY, anim);

        int hintA = clamp((int)(anim * 160));
        drawCenteredString(ms, this.font, "LMB toggle  |  RMB settings  |  Scroll = scroll",
                px + PANEL_W / 2 - 48, py + PANEL_H - 13, (hintA << 24) | 0xAAAAAA);

        ms.popPose();

        if (closing && anim <= 0.001F) Minecraft.getInstance().setScreen(null);
    }

    private void drawCards(MatrixStack ms, int mouseX, int mouseY, float anim) {
        int scroll = (int) scrollOffset;
        for (int i = 0; i < visibleFeatures.size(); i++) {
            FeatureType f  = visibleFeatures.get(i);
            int cy = cardY + i * (CARD_H + CARD_GAP) - scroll;

            // Skip if fully outside card area
            if (cy + CARD_H < cardY || cy > cardY + cardAreaH) continue;

            // Fade только снизу когда скроллим (сверху не нужно — режет первые карточки)
            float visBot = cardY + cardAreaH;
            float fadeRange = 10F;
            float botFade = (cy + CARD_H) > visBot - fadeRange
                    ? Math.max(0F, (visBot - (cy + CARD_H)) / fadeRange) : 1F;
            float fade = botFade;

            drawFeatureCard(ms, mouseX, mouseY, cardX, cy, cardW, CARD_H, f, anim * fade);
        }
    }

    private void drawFeatureCard(MatrixStack ms, int mx, int my,
                                 int x, int y, int w, int h,
                                 FeatureType feature, float alpha) {
        if (alpha <= 0) return;
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean on    = CosmeticsState.get().isOn(feature);
        float   hov   = hover ? 1F : 0F;

        ThemeManager tm = ThemeManager.get();
        int base = blendColor(tm.cardHover(), tm.cardOn(), hov);
        fill(ms, x, y, x + w, y + h, withAlpha(base, alpha));

        if (on) {
            fill(ms, x, y, x + 3, y + h, withAlpha(0xFF000000 | (tm.accent() & 0xFFFFFF), alpha));
            fill(ms, x + 3, y, x + 5, y + h, withAlpha(0x50000000 | (tm.accent() & 0xFFFFFF), alpha));
        }
        if (hover) fill(ms, x, y, x + w, y + 1, withAlpha(0xFF000000 | (tm.accent() & 0xFFFFFF), alpha * 0.5F));

        drawString(ms, font, feature.displayName, x + 10, y + (h - 8) / 2,
                withAlpha(on ? (0xFF000000 | (tm.activeText() & 0xFFFFFF)) : 0xFFCCCCCC, alpha));

        // Pill toggle
        int pillW = 28, pillH = 12;
        int pX = x + w - pillW - 8;
        int pY = y + (h - pillH) / 2;
        fill(ms, pX, pY, pX + pillW, pY + pillH,
                on ? withAlpha(0xFF000000 | (tm.accent() & 0xFFFFFF), alpha)
                   : withAlpha(0xFF3A3650, alpha));
        int knobX = on ? pX + pillW - pillH + 1 : pX + 1;
        fill(ms, knobX, pY + 1, knobX + pillH - 2, pY + pillH - 1, withAlpha(0xFFFFFFFF, alpha));
    }

    private void drawTabScrollbar(MatrixStack ms, float anim) {
        float maxTabScroll = Math.max(0F, tabContentH - tabAreaH);
        if (maxTabScroll <= 0) return;

        int sbX = px + 8 + TAB_W + 1;
        int sbY = py + 44;
        int sbH = tabAreaH;

        int track = 0xFF000000 | (ThemeManager.get().panelBot() & 0xFFFFFF);
        fill(ms, sbX, sbY, sbX + TAB_SCROLL_W, sbY + sbH, withAlpha(track, anim));

        float ratio  = (float) tabAreaH / tabContentH;
        int thumbH   = Math.max(10, (int)(sbH * ratio));
        float frac   = tabScrollOffset / maxTabScroll;
        int thumbY   = sbY + (int)((sbH - thumbH) * frac);

        int thumb = 0xFF000000 | (ThemeManager.get().accent() & 0xFFFFFF);
        fill(ms, sbX + 1, thumbY, sbX + TAB_SCROLL_W - 1, thumbY + thumbH,
                withAlpha(thumb, anim * 0.7F));
    }

    private void drawScrollbar(MatrixStack ms, float anim) {
        if (maxScroll() <= 0) return;

        int sbX  = cardX + cardW + 4;
        int sbY  = cardY;
        int sbH  = cardAreaH;

        ThemeManager tm = ThemeManager.get();
        int track = 0xFF000000 | (tm.panelBot() & 0xFFFFFF);
        fill(ms, sbX, sbY, sbX + SCROLL_W, sbY + sbH, withAlpha(track, anim));

        float ratio      = (float) cardAreaH / contentHeight;
        int   thumbH     = Math.max(16, (int)(sbH * ratio));
        float scrollFrac = scrollOffset / maxScroll();
        int   thumbY     = sbY + (int)((sbH - thumbH) * scrollFrac);

        boolean hoverSb = isMouseOverScrollbar(
                Minecraft.getInstance().mouseHandler.xpos(),
                Minecraft.getInstance().mouseHandler.ypos());

        int accent = 0xFF000000 | (tm.accent() & 0xFFFFFF);
        int thumbCol = hoverSb || draggingScroll ? accent
                : (0xFF000000 | (blendColor(tm.panelTop(), tm.accent(), 0.4F) & 0xFFFFFF));
        fill(ms, sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + thumbH, withAlpha(thumbCol, anim));

        // Thumb top accent line
        int accentLight = 0xFF000000 | (blendColor(tm.accent(), 0xFFFFFF, 0.3F) & 0xFFFFFF);
        fill(ms, sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + 2, withAlpha(accentLight, anim));
    }

    // ---- Input -----------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        // Если мышь над зоной табов — скроллим табы
        float maxTabScroll = Math.max(0F, tabContentH - tabAreaH);
        boolean overTabs = mx >= px + 8 && mx <= px + 8 + TAB_W + TAB_SCROLL_W
                        && my >= py + 44 && my <= py + 44 + tabAreaH;
        if (overTabs && maxTabScroll > 0) {
            tabScrollTarget -= (float)(delta * 24);
            tabScrollTarget  = Math.max(0, Math.min(maxTabScroll, tabScrollTarget));
        } else {
            scrollTarget -= (float)(delta * (CARD_H + CARD_GAP));
            scrollTarget  = Math.max(0, Math.min(maxScroll(), scrollTarget));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return false;

        // Scrollbar click
        if (button == 0 && isMouseOverScrollbar(mx, my)) {
            draggingScroll = true;
            dragStartY     = my;
            dragStartOffset = scrollOffset;
            return true;
        }

        if (githubButton  != null && githubButton.contains(mx, my))  { openRepo(); return true; }
        if (themeButton   != null && themeButton.contains(mx, my))   {
            ThemeManager.get().next();
            ConfigManager.get().save();
            return true;
        }
        if (hudEditButton != null && hudEditButton.contains(mx, my)) {
            Minecraft.getInstance().setScreen(new HudEditScreen());
            return true;
        }

        for (CategoryTab c : categoryTabs) {
            if (c.containsScrolled(mx, my, (int) tabScrollOffset,
                                   py + 44, py + 44 + tabAreaH)) {
                current = c.category;
                rebuildFeatures();
                return true;
            }
        }

        // Card clicks — use scroll-adjusted positions
        int scroll = (int) scrollOffset;
        for (int i = 0; i < visibleFeatures.size(); i++) {
            FeatureType f = visibleFeatures.get(i);
            int cy = cardY + i * (CARD_H + CARD_GAP) - scroll;
            // Только проверяем что карточка видима (хоть частично в зоне)
            if (cy + CARD_H < cardY || cy > cardY + cardAreaH) continue;
            if (mx >= cardX && mx <= cardX + cardW && my >= cy && my <= cy + CARD_H) {
                if (button == 0) {
                    CosmeticsState.get().toggle(f);
                } else if (button == 1) {
                    Minecraft.getInstance().setScreen(new SettingsScreen(f));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScroll && button == 0 && maxScroll() > 0) {
            int sbH     = cardAreaH;
            float ratio = (float) contentHeight / sbH;
            float delta = (float)(my - dragStartY) * ratio;
            scrollTarget = Math.max(0, Math.min(maxScroll(), dragStartOffset + delta));
            scrollOffset = scrollTarget; // snap while dragging
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) draggingScroll = false;
        return super.mouseReleased(mx, my, button);
    }

    private boolean isMouseOverScrollbar(double mx, double my) {
        if (maxScroll() <= 0) return false;
        int sbX = cardX + cardW + 4;
        return mx >= sbX && mx <= sbX + SCROLL_W && my >= cardY && my <= cardY + cardAreaH;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 344) {
            closing = true; closingAtMs = System.currentTimeMillis(); return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void onClose() {
        ConfigManager.get().save();
        if (!closing) { closing = true; closingAtMs = System.currentTimeMillis(); }
        else super.onClose();
    }

    private void openRepo() {
        Minecraft mc = Minecraft.getInstance();
        final MainMenuScreen self = this;
        mc.setScreen(new ConfirmOpenLinkScreen(ok -> {
            if (ok) try { Util.getPlatform().openUri(new URI(REPO_URL)); } catch (Exception ignored) {}
            mc.setScreen(self);
        }, REPO_URL, true));
    }

    private float animProgress() {
        long now = System.currentTimeMillis();
        if (!closing) return easeOut(Math.min(1F, (now - openedAtMs) / (float) ANIM_MS));
        else          return 1F - easeOut(Math.min(1F, (now - closingAtMs) / (float) ANIM_MS));
    }
    private static float easeOut(float t) { return 1F - (1F - t) * (1F - t); }

    // ---- Widgets ---------------------------------------------------------------

    private class CategoryTab {
        final int x, y, w, h; final FeatureType.Category category;
        CategoryTab(int x, int y, int w, int h, FeatureType.Category c) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.category=c;
        }
        // Проверка клика с учётом скролла и видимой зоны
        boolean containsScrolled(double mx, double my, int scroll, int clipTop, int clipBot) {
            int ry = y - scroll;
            if (ry + h < clipTop || ry > clipBot) return false;
            return mx >= x && mx <= x + w && my >= ry && my <= ry + h;
        }
        boolean contains(double mx, double my) { return mx>=x&&mx<=x+w&&my>=y&&my<=y+h; }

        void draw(MatrixStack ms, int mx, int my, float alpha,
                  FeatureType.Category sel, int scroll) {
            int ry = y - scroll;
            int clipTop = py + 44, clipBot = py + 44 + tabAreaH;
            if (ry + h < clipTop || ry > clipBot) return;

            float fadeRange = 8F;
            float topFade = ry < clipTop + fadeRange
                    ? Math.max(0F, (ry - clipTop) / fadeRange) : 1F;
            float botFade = (ry + h) > clipBot - fadeRange
                    ? Math.max(0F, (clipBot - (ry + h)) / fadeRange) : 1F;
            float fade = Math.min(topFade, botFade);

            boolean hover    = mx >= x && mx <= x + w && my >= ry && my <= ry + h;
            boolean selected = sel == category;
            float   hov      = hover ? 1F : 0F;

            ThemeManager tm = ThemeManager.get();
            int bgCol = selected ? tm.tabSel() : blendColor(tm.cardHover(), tm.cardOn(), hov);
            fill(ms, x, ry, x+w, ry+h, withAlpha(bgCol, alpha*fade));

            int accent = 0xFF000000 | (tm.accent() & 0xFFFFFF);
            if (selected) {
                fill(ms, x, ry, x+3, ry+h, withAlpha(accent, alpha*fade));
                fill(ms, x+3, ry, x+4, ry+h, withAlpha(0x40000000 | (tm.accent() & 0xFFFFFF), alpha*fade));
            } else if (hov > 0) {
                fill(ms, x, ry, x+2, ry+h, withAlpha(accent, alpha*hov*fade*0.6F));
            }
            String label = category.name().charAt(0)
                    + category.name().substring(1).toLowerCase();
            int textCol = selected
                    ? (0xFF000000 | (tm.activeText() & 0xFFFFFF))
                    : 0xFFCCCCCC;
            drawString(ms, font, label, x+10, ry+(h-8)/2, withAlpha(textCol, alpha*fade));
        }
    }

    private class GitHubButton {
        final int x, y, w, h;
        GitHubButton(int x, int y, int w, int h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        boolean contains(double mx, double my) { return mx>=x&&mx<=x+w&&my>=y&&my<=y+h; }
        void draw(MatrixStack ms, int mx, int my, float alpha) {
            boolean hover = contains(mx,my);
            float hov = hover?1F:0F;
            int top = blendColor(0xFF2A1E4A,0xFF4B2E90,hov);
            int bot = blendColor(0xFF180F2A,0xFF2A1D55,hov);
            fillGradient(ms,x,y,x+w,y+h, withAlpha(top,alpha), withAlpha(bot,alpha));
            fill(ms,x,y,x+w,y+1, withAlpha(0xFFB98FFF,alpha*(0.5F+0.5F*hov)));
            String label = "\u2605 GitHub";
            font.drawShadow(ms,label, x+(w-font.width(label))/2, y+(h-8)/2, withAlpha(0xFFFFFFFF,alpha));
        }
    }

    private class ThemeButton {
        final int x, y, w, h;
        ThemeButton(int x, int y, int w, int h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        boolean contains(double mx, double my) { return mx>=x&&mx<=x+w&&my>=y&&my<=y+h; }
        void draw(MatrixStack ms, int mx, int my, float alpha) {
            boolean hover = contains(mx, my);
            ThemeManager tm = ThemeManager.get();
            int accent = tm.accent();
            int top = hover ? blendColor(tm.panelTop(), accent, 0.25F) : tm.panelTop();
            int bot = hover ? blendColor(tm.panelBot(), accent, 0.15F) : tm.panelBot();
            fillGradient(ms, x, y, x+w, y+h, withAlpha(top, alpha), withAlpha(bot, alpha));
            fill(ms, x, y, x+w, y+1, withAlpha(accent, alpha * (hover ? 0.9F : 0.5F)));
            String label = "\u25D0 " + ThemeManager.THEME_NAMES[tm.getCurrentThemeIndex()];
            font.drawShadow(ms, label, x+(w-font.width(label))/2, y+(h-8)/2,
                    withAlpha(0xFFFFFFFF, alpha));
        }
    }

    private class HudEditButton {
        final int x, y, w, h;
        HudEditButton(int x, int y, int w, int h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        boolean contains(double mx, double my) { return mx>=x&&mx<=x+w&&my>=y&&my<=y+h; }
        void draw(MatrixStack ms, int mx, int my, float alpha) {
            boolean hover = contains(mx, my);
            ThemeManager tm = ThemeManager.get();
            int accent = tm.accent();
            int top = hover ? blendColor(tm.panelTop(), accent, 0.25F) : tm.panelTop();
            int bot = hover ? blendColor(tm.panelBot(), accent, 0.15F) : tm.panelBot();
            fillGradient(ms, x, y, x+w, y+h, withAlpha(top, alpha), withAlpha(bot, alpha));
            fill(ms, x, y, x+w, y+1, withAlpha(accent, alpha * (hover ? 0.9F : 0.5F)));
            String label = "\u2750 HUD Edit";
            font.drawShadow(ms, label, x+(w-font.width(label))/2, y+(h-8)/2,
                    withAlpha(0xFFFFFFFF, alpha));
        }
    }


    // ---- Colour helpers --------------------------------------------------------
    private static int withAlpha(int argb, float alpha) {
        int a = clamp((int)((argb>>>24&0xFF)*alpha));
        return (a<<24)|(argb&0x00FFFFFF);
    }
    private static int withAlpha(int argb, int a) { return (clamp(a)<<24)|(argb&0x00FFFFFF); }
    private static int blendColor(int c1, int c2, float t) {
        t = Math.max(0,Math.min(1,t));
        int r=(int)(((c1>>16)&0xFF)*(1-t)+((c2>>16)&0xFF)*t);
        int g=(int)(((c1>>8)&0xFF)*(1-t)+((c2>>8)&0xFF)*t);
        int b=(int)((c1&0xFF)*(1-t)+(c2&0xFF)*t);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }
    private static int clamp(int v) { return Math.max(0,Math.min(255,v)); }
}

package com.example.cosmetics.config;

/**
 * Global GUI theme. Controls the colour palette used throughout
 * MainMenuScreen, HUD panels, scrollbars, and card highlights.
 *
 * Themes:
 *   0 = NEON      — classic purple/violet  (original look)
 *   1 = OCEAN     — deep blue / cyan
 *   2 = FIRE      — dark red / orange
 *   3 = MINT      — dark green / lime
 *   4 = ROSE      — dark pink / magenta
 *   5 = GOLD      — dark brown / yellow-gold
 *   6 = MONOCHROME— pure grey / white
 */
public final class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();
    public static ThemeManager get() { return INSTANCE; }

    public static final int THEME_COUNT = 7;
    public static final String[] THEME_NAMES = {
        "Neon", "Ocean", "Fire", "Mint", "Rose", "Gold", "Mono"
    };

    // Per-theme palette: { panelTop, panelBot, accent, tabSel, cardOn, cardHover }
    private static final int[][] PALETTES = {
        // NEON
        { 0x171325, 0x0D0B1A, 0x9B6DFF, 0x2B2550, 0x201C38, 0x16132A },
        // OCEAN
        { 0x0A1A2E, 0x061426, 0x2BA7FF, 0x0E2040, 0x0E1E36, 0x081420 },
        // FIRE
        { 0x2A0F08, 0x180603, 0xFF7A2A, 0x3A1208, 0x2A1008, 0x1E0A04 },
        // MINT
        { 0x08291E, 0x04170F, 0x3CFFA8, 0x0E3A22, 0x0E2A1A, 0x081A10 },
        // ROSE
        { 0x2A0818, 0x180410, 0xFF5CA8, 0x3A0E22, 0x2A0E1A, 0x1E0810 },
        // GOLD
        { 0x1E1608, 0x120D04, 0xFFCC44, 0x2E200A, 0x201808, 0x161004 },
        // MONO
        { 0x1A1A1A, 0x0E0E0E, 0xCCCCCC, 0x2E2E2E, 0x242424, 0x181818 },
    };

    private int currentTheme = 0;

    private ThemeManager() {}

    public int getCurrentThemeIndex() { return currentTheme; }

    public void setTheme(int index) {
        currentTheme = Math.floorMod(index, THEME_COUNT);
    }

    public void next() { setTheme(currentTheme + 1); }
    public void prev() { setTheme(currentTheme - 1); }

    // ---- Palette accessors --------------------------------------------------

    /** Dark gradient top colour for panels. */
    public int panelTop()   { return PALETTES[currentTheme][0]; }
    /** Dark gradient bottom colour for panels. */
    public int panelBot()   { return PALETTES[currentTheme][1]; }
    /** Accent / highlight colour (used for glow, scrollbar thumb, active card strip). */
    public int accent()     { return PALETTES[currentTheme][2]; }
    /** Selected tab background. */
    public int tabSel()     { return PALETTES[currentTheme][3]; }
    /** Active feature card background. */
    public int cardOn()     { return PALETTES[currentTheme][4]; }
    /** Hovered card background. */
    public int cardHover()  { return PALETTES[currentTheme][5]; }

    /** Active feature name text colour (always derived from accent, lightened). */
    public int activeText() {
        int a = accent();
        int r = Math.min(255, ((a >> 16) & 0xFF) + 60);
        int g = Math.min(255, ((a >>  8) & 0xFF) + 60);
        int b = Math.min(255, ( a        & 0xFF) + 60);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}

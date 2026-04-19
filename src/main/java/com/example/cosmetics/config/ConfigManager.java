package com.example.cosmetics.config;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Saves and loads all feature states + settings to/from
 * .minecraft/cosmetics_config.json  (simple hand-rolled JSON — no Gson dep).
 *
 * Call ConfigManager.load() once on startup (FMLClientSetupEvent).
 * Call ConfigManager.save() whenever settings change (screen close, toggle).
 */
public final class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();
    public static ConfigManager get() { return INSTANCE; }

    private Path configPath;

    private ConfigManager() {}

    // ---- public API ---------------------------------------------------------

    public void init() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        configPath = gameDir.toPath().resolve("cosmetics_config.json");
    }

    public void save() {
        if (configPath == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"enabled\": [");
            boolean firstE = true;
            CosmeticsState state = CosmeticsState.get();
            for (FeatureType f : FeatureType.values()) {
                if (state.isOn(f)) {
                    if (!firstE) sb.append(", ");
                    sb.append("\"").append(f.name()).append("\"");
                    firstE = false;
                }
            }
            sb.append("],\n");
            sb.append("  \"settings\": {\n");
            boolean firstS = true;
            for (FeatureType f : FeatureType.values()) {
                FeatureSettings s = state.settings(f);
                if (!firstS) sb.append(",\n");
                sb.append("    \"").append(f.name()).append("\": ");
                sb.append(settingsToJson(s));
                firstS = false;
            }
            sb.append("\n  },\n");

            // HUD positions
            sb.append("  \"hudX\": ").append(HudPositionManager.get().getCosmeticsX()).append(",\n");
            sb.append("  \"hudY\": ").append(HudPositionManager.get().getCosmeticsY()).append(",\n");
            sb.append("  \"targetHudX\": ").append(HudPositionManager.get().getTargetX()).append(",\n");
            sb.append("  \"targetHudY\": ").append(HudPositionManager.get().getTargetY()).append(",\n");

            // Theme
            sb.append("  \"theme\": ").append(ThemeManager.get().getCurrentThemeIndex()).append("\n");
            sb.append("}\n");

            Files.write(configPath, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("[CosmeticsMod] Failed to save config: " + e.getMessage());
        }
    }

    public void load() {
        if (configPath == null) return;
        if (!Files.exists(configPath)) return;
        try {
            String raw = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            CosmeticsState state = CosmeticsState.get();

            // Parse enabled list
            String enabledBlock = extractBlock(raw, "\"enabled\": [", "]");
            if (enabledBlock != null) {
                // First disable all
                for (FeatureType f : FeatureType.values()) {
                    if (state.isOn(f)) state.toggle(f);
                }
                // Re-enable saved ones
                for (FeatureType f : FeatureType.values()) {
                    if (enabledBlock.contains("\"" + f.name() + "\"")) {
                        state.toggle(f);
                    }
                }
            }

            // Parse settings
            for (FeatureType f : FeatureType.values()) {
                String key = "\"" + f.name() + "\": {";
                int idx = raw.indexOf(key);
                if (idx == -1) continue;
                int start = raw.indexOf('{', idx);
                int end   = raw.indexOf('}', start);
                if (start == -1 || end == -1) continue;
                String block = raw.substring(start + 1, end);
                parseSettings(block, state.settings(f));
            }

            // HUD positions
            float hx = parseFloat(raw, "\"hudX\":");
            float hy = parseFloat(raw, "\"hudY\":");
            float tx = parseFloat(raw, "\"targetHudX\":");
            float ty = parseFloat(raw, "\"targetHudY\":");
            if (hx >= 0) HudPositionManager.get().setCosmeticsPos((int)hx, (int)hy);
            if (tx >= 0) HudPositionManager.get().setTargetPos((int)tx, (int)ty);

            // Theme
            int theme = (int) parseFloat(raw, "\"theme\":");
            if (theme >= 0) ThemeManager.get().setTheme(theme);

        } catch (Exception e) {
            System.err.println("[CosmeticsMod] Failed to load config: " + e.getMessage());
        }
    }

    // ---- JSON helpers -------------------------------------------------------

    private static String settingsToJson(FeatureSettings s) {
        return "{"
            + "\"colorR\":" + s.colorR + ","
            + "\"colorG\":" + s.colorG + ","
            + "\"colorB\":" + s.colorB + ","
            + "\"size\":"   + s.size   + ","
            + "\"density\":" + s.density + ","
            + "\"speed\":"  + s.speed  + ","
            + "\"style\":"  + s.style  + ","
            + "\"count\":"  + s.count  + ","
            + "\"offsetY\":" + s.offsetY + ","
            + "\"killAuraAutoCrit\":" + s.killAuraAutoCrit + ","
            + "\"killAuraTargetPlayers\":" + s.killAuraTargetPlayers + ","
            + "\"killAuraTargetHostile\":" + s.killAuraTargetHostile + ","
            + "\"killAuraTargetPassive\":" + s.killAuraTargetPassive + ","
            + "\"killAuraSwing\":" + s.killAuraSwing + ","
            + "\"killAuraRaytrace\":" + s.killAuraRaytrace + ","
            + "\"killAuraRotMode\":" + s.killAuraRotMode + ","
            + "\"killAuraSortMode\":" + s.killAuraSortMode + ","
            + "\"killAuraFov\":" + s.killAuraFov + ","
            + "\"killAuraAttackDelay\":" + s.killAuraAttackDelay + ","
            + "\"killAuraMinRange\":" + s.killAuraMinRange + ","
            + "\"strafeMode\":" + s.strafeMode + ","
            + "\"strafeDirection\":" + s.strafeDirection + ","
            + "\"strafeRadius\":" + s.strafeRadius + ","
            + "\"strafeOnlyInCombat\":" + s.strafeOnlyInCombat + ","
            + "\"strafeSprint\":" + s.strafeSprint + ","
            + "\"strafeJitter\":" + s.strafeJitter
            + "}";
    }

    private static void parseSettings(String block, FeatureSettings s) {
        s.colorR  = getFloat(block, "colorR",  s.colorR);
        s.colorG  = getFloat(block, "colorG",  s.colorG);
        s.colorB  = getFloat(block, "colorB",  s.colorB);
        s.size    = getFloat(block, "size",    s.size);
        s.density = getFloat(block, "density", s.density);
        s.speed   = getFloat(block, "speed",   s.speed);
        s.style   = (int) getFloat(block, "style",   s.style);
        s.count   = (int) getFloat(block, "count",   s.count);
        s.offsetY = getFloat(block, "offsetY", s.offsetY);
        s.killAuraAutoCrit      = getBool(block, "killAuraAutoCrit",      s.killAuraAutoCrit);
        s.killAuraTargetPlayers = getBool(block, "killAuraTargetPlayers", s.killAuraTargetPlayers);
        s.killAuraTargetHostile = getBool(block, "killAuraTargetHostile", s.killAuraTargetHostile);
        s.killAuraTargetPassive = getBool(block, "killAuraTargetPassive", s.killAuraTargetPassive);
        s.killAuraSwing         = getBool(block, "killAuraSwing",         s.killAuraSwing);
        s.killAuraRaytrace      = getBool(block, "killAuraRaytrace",      s.killAuraRaytrace);
        s.killAuraRotMode       = (int) getFloat(block, "killAuraRotMode",  s.killAuraRotMode);
        s.killAuraSortMode      = (int) getFloat(block, "killAuraSortMode", s.killAuraSortMode);
        s.killAuraFov           = getFloat(block, "killAuraFov",           s.killAuraFov);
        s.killAuraAttackDelay   = (int) getFloat(block, "killAuraAttackDelay", s.killAuraAttackDelay);
        s.killAuraMinRange      = getFloat(block, "killAuraMinRange",      s.killAuraMinRange);
        s.strafeMode            = (int) getFloat(block, "strafeMode",       s.strafeMode);
        s.strafeDirection       = (int) getFloat(block, "strafeDirection",  s.strafeDirection);
        s.strafeRadius          = getFloat(block, "strafeRadius",           s.strafeRadius);
        s.strafeOnlyInCombat    = getBool(block, "strafeOnlyInCombat",      s.strafeOnlyInCombat);
        s.strafeSprint          = getBool(block, "strafeSprint",            s.strafeSprint);
        s.strafeJitter          = getBool(block, "strafeJitter",            s.strafeJitter);
    }

    private static float getFloat(String json, String key, float def) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i == -1) return def;
        int start = i + k.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Float.parseFloat(json.substring(start, end)); }
        catch (Exception e) { return def; }
    }

    private static boolean getBool(String json, String key, boolean def) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i == -1) return def;
        String rest = json.substring(i + k.length()).trim();
        if (rest.startsWith("true"))  return true;
        if (rest.startsWith("false")) return false;
        return def;
    }

    private static float parseFloat(String json, String key) {
        return getFloat(json, key.replace("\"", "").replace(":", ""), -1);
    }

    private static String extractBlock(String json, String startMarker, String endMarker) {
        int s = json.indexOf(startMarker);
        if (s == -1) return null;
        s += startMarker.length();
        int e = json.indexOf(endMarker, s);
        if (e == -1) return null;
        return json.substring(s, e);
    }
}

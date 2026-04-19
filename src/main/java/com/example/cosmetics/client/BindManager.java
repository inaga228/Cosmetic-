package com.example.cosmetics.client;

import com.example.cosmetics.feature.FeatureType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Позволяет привязать любую фичу к произвольной клавише (GLFW key code).
 * -1 = нет бинда.
 */
public final class BindManager {

    private static final BindManager INSTANCE = new BindManager();
    public static BindManager get() { return INSTANCE; }

    // feature -> GLFW key code (-1 = не привязано)
    private final Map<FeatureType, Integer> binds = new EnumMap<>(FeatureType.class);

    // Состояние клавиш на прошлом тике (чтобы реагировать только на нажатие, не удержание)
    private final Map<Integer, Boolean> prevState = new java.util.HashMap<>();

    private BindManager() {}

    /** Установить бинд. key = GLFW_KEY_*, или -1 чтобы убрать. */
    public void setBind(FeatureType feature, int glfwKey) {
        if (glfwKey == -1) binds.remove(feature);
        else binds.put(feature, glfwKey);
    }

    /** Получить привязанную клавишу для фичи, или -1. */
    public int getBind(FeatureType feature) {
        return binds.getOrDefault(feature, -1);
    }

    /** Убрать бинд с фичи. */
    public void clearBind(FeatureType feature) {
        binds.remove(feature);
    }

    /** Проверить есть ли бинд. */
    public boolean hasBind(FeatureType feature) {
        return binds.containsKey(feature);
    }

    /**
     * Вызывается каждый тик — проверяет нажатия и переключает фичи.
     * Возвращает фичу которую только что переключили (или null).
     */
    public FeatureType tick(long windowHandle) {
        CosmeticsState state = CosmeticsState.get();
        FeatureType toggled = null;

        for (Map.Entry<FeatureType, Integer> entry : binds.entrySet()) {
            int key = entry.getValue();
            boolean pressed = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, key)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            boolean wasPressed = prevState.getOrDefault(key, false);

            if (pressed && !wasPressed) {
                state.toggle(entry.getKey());
                toggled = entry.getKey();
            }
            prevState.put(key, pressed);
        }
        return toggled;
    }

    /** Все текущие бинды (для отображения в GUI). */
    public Map<FeatureType, Integer> allBinds() {
        return java.util.Collections.unmodifiableMap(binds);
    }

    /**
     * Красивое название клавиши по GLFW коду.
     * Например: 65 -> "A", 256 -> "ESC", 341 -> "CTRL"
     */
    public static String keyName(int glfwKey) {
        if (glfwKey == -1) return "None";
        switch (glfwKey) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE:       return "ESC";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB:          return "TAB";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK:    return "CAPS";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT:   return "L-SHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT:  return "R-SHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL: return "L-CTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL:return "R-CTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT:     return "L-ALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT:    return "R-ALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE:        return "SPACE";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER:        return "ENTER";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:    return "BKSP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE:       return "DEL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_INSERT:       return "INS";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME:         return "HOME";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_END:          return "END";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP:      return "PG-UP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN:    return "PG-DN";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP:           return "UP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN:         return "DOWN";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT:         return "LEFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT:        return "RIGHT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F1:           return "F1";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F2:           return "F2";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F3:           return "F3";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F4:           return "F4";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F5:           return "F5";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F6:           return "F6";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F7:           return "F7";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F8:           return "F8";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F9:           return "F9";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F10:          return "F10";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F11:          return "F11";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F12:          return "F12";
            default:
                // Буквы и цифры
                String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(glfwKey, 0);
                if (name != null && !name.isEmpty()) return name.toUpperCase();
                return "KEY-" + glfwKey;
        }
    }
}

package com.example.cosmetics.client;

import com.example.cosmetics.feature.FeatureType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Паник-система: пишешь .panic в чат — все боевые и entity фичи выключаются.
 * Пишешь снова — возвращаются обратно.
 */
public final class PanicManager {

    private static final PanicManager INSTANCE = new PanicManager();
    public static PanicManager get() { return INSTANCE; }

    /** Фичи которые скрываются при панике */
    private static final Set<FeatureType> PANIC_FEATURES = EnumSet.of(
        // Combat
        FeatureType.KILL_AURA,
        FeatureType.CRIT,
        FeatureType.AUTO_CLICKER,
        FeatureType.SMOOTH_AIM,
        FeatureType.STRAFE,
        FeatureType.ANTI_BOT,
        FeatureType.AUTO_POT,
        FeatureType.AUTO_GAP,
        FeatureType.NO_FIRE_OVERLAY,
        // Utility (тоже подозрительные)
        FeatureType.AUTO_SPRINT,
        FeatureType.FULLBRIGHT,
        FeatureType.AUTO_TOTEM
    );

    private boolean panicActive = false;
    // Фичи которые были включены до паники — чтобы вернуть их
    private final Set<FeatureType> savedEnabled = EnumSet.noneOf(FeatureType.class);

    private PanicManager() {}

    public boolean isPanicActive() { return panicActive; }

    /**
     * Переключает панику. Возвращает true если теперь паника активна.
     */
    public boolean toggle() {
        if (!panicActive) {
            activatePanic();
        } else {
            deactivatePanic();
        }
        return panicActive;
    }

    private void activatePanic() {
        CosmeticsState state = CosmeticsState.get();
        savedEnabled.clear();
        for (FeatureType f : PANIC_FEATURES) {
            if (state.isOn(f)) {
                savedEnabled.add(f);  // запомнить что было включено
                state.toggle(f);      // выключить
            }
        }
        panicActive = true;
    }

    private void deactivatePanic() {
        CosmeticsState state = CosmeticsState.get();
        for (FeatureType f : savedEnabled) {
            if (!state.isOn(f)) {
                state.toggle(f);  // вернуть обратно
            }
        }
        savedEnabled.clear();
        panicActive = false;
    }
}

package com.example.cosmetics.feature;

/**
 * Mutable settings bag for a feature.
 */
public final class FeatureSettings {
    public float colorR = 1.0F;
    public float colorG = 1.0F;
    public float colorB = 1.0F;

    public float size    = 1.0F;
    public float density = 1.0F;
    public float speed   = 1.0F;

    public int style = 0;
    public int count = 8;

    public float offsetX = 0.0F, offsetY = 0.0F, offsetZ = 0.0F;
    public float rotX    = 0.0F, rotY    = 0.0F, rotZ    = 0.0F;

    // ---- Kill Aura ----------------------------------------------------------
    public boolean killAuraAutoCrit      = false;
    public boolean killAuraTargetPlayers = false;
    public boolean killAuraTargetHostile = true;
    public boolean killAuraTargetPassive = false;
    public boolean killAuraAntiBot       = false;
    public int     killAuraRotMode       = 0; // 0=BodyTrack 1=FullLock 2=Silent
    public int     killAuraSortMode      = 0; // 0=Closest 1=LowestHP 2=HighestHP

    // ---- Trails -------------------------------------------------------------
    public boolean trailOnlyWhenMoving = false;
    public int     trailFadeMode       = 0; // 0=Solid 1=Fade 2=Rainbow

    // ---- Auras (Particles) --------------------------------------------------
    public boolean auraRotate          = true;
    public int     auraShape           = 0;  // 0=Orbit 1=Random 2=Pulse

    // ---- Wings --------------------------------------------------------------
    public boolean wingsOnlySprinting  = false;
    public boolean wingsFlapAnim       = true;

    // ---- Smooth Aim ---------------------------------------------------------
    public boolean smoothAimOnlyPlayers     = true;
    public boolean smoothAimNeedWeapon      = false;

    // ---- Auto Clicker -------------------------------------------------------
    public boolean clickerOnlyInRange       = false;

    // ---- Strafe -------------------------------------------------------------
    public int     strafeDirection          = 2; // 0=Left 1=Right 2=Random
    public int     strafeMode               = 0; // 0=Circle 1=Side 2=Zigzag
    public float   strafeRadius             = 2.5F;
    public boolean strafeOnlyInCombat       = true;
    public boolean strafeSprint             = true;
    public boolean strafeJitter             = false; // мелкое смещение против предсказания

    // ---- Kill Aura extended -------------------------------------------------
    public float   killAuraMinRange         = 0.5F;
    public boolean killAuraIgnoreWalls      = false;
    public boolean killAuraSwing            = true;
    public boolean killAuraRaytrace         = false; // только при прямой видимости
    public int     killAuraAttackDelay      = 0;   // доп. задержка мс (0 = выкл)
    public float   killAuraAttackDelayRand  = 0F;  // рандом ±мс сверху задержки
    public boolean killAuraSilentRotation   = false;
    public float   killAuraFov              = 180F; // угол обзора для поиска цели (180=любое направление)

    // ---- Kill Aura (из cheatutils) ------------------------------------------
    // delayMode: 0=ConstDelay (интервал в тиках), 1=Cooldown (ждать 100% заряда)
    public int     killAuraDelayMode        = 0;
    // attackTickInterval — каждые N тиков бить (только в режиме ConstDelay)
    public int     killAuraTickInterval     = 5;
    // extraTicks — смещение: чуть раньше 100% заряда (-) или позже (+) (только Cooldown)
    public float   killAuraExtraTicks       = 0F;
    // attackAll — мульти-аура: бить ВСЕХ в радиусе за один тик, а не только ближайшего
    public boolean killAuraAttackAll        = false;
    // doNotAttackOnItemUse — не атаковать пока игрок использует предмет (лук, еда...)
    public boolean killAuraNoAttackOnUse    = false;
    // maxVerticalAngle — отдельный FOV по вертикали (0 = выкл, используется killAuraFov)
    public float   killAuraVerticalFov      = 360F;

    // ---- HUD ----------------------------------------------------------------
    public boolean hudOnlyWhenActive        = false;

    // ---- Auto Totem ---------------------------------------------------------
    public boolean totemShowAlert           = true;

    // ---- TriggerBot ---------------------------------------------------------
    public long    triggerDelay         = 80L;   // мс задержки перед кликом
    public float   triggerDelayRand     = 40F;   // рандом ±мс
    public float   triggerFov           = 10F;   // угол прицела в градусах
    public boolean triggerTargetPlayers = true;
    public boolean triggerTargetHostile = true;
    public boolean triggerTargetPassive = false;
    public boolean triggerNeedWeapon    = true;
    public boolean triggerOnlyCharged   = true;
    public boolean triggerAutoRelease   = true;  // true=одиночные клики, false=удержание
    public boolean triggerCancelOnMove  = false;

    // ---- BowAimbot ----------------------------------------------------------
    public float   bowAimFov            = 60F;   // угол поиска цели
    public float   bowAimSpeed          = 8F;    // скорость наведения
    public float   bowAimLead           = 1.0F;  // множитель опережения (0=нет, 1=полное)
    public float   bowAimGravity        = 1.0F;  // множитель гравитации
    public int     bowAimPredictSteps   = 4;     // кол-во итераций предсказания
    public boolean bowAimTargetPlayers  = true;
    public boolean bowAimTargetHostile  = true;
    public boolean bowAimOnlyCharged    = true;  // только при полном заряде

    // ---- Hitbox -------------------------------------------------------------
    public float   hitboxX              = 1.5F;  // множитель по X
    public float   hitboxY              = 1.2F;  // множитель по Y
    public float   hitboxZ              = 1.5F;  // множитель по Z
    public boolean hitboxTargetPlayers  = true;
    public boolean hitboxTargetHostile  = true;
    public boolean hitboxTargetPassive  = false;
    public boolean hitboxOnlyInCombat   = false;
    public boolean hitboxDebug          = false; // отрисовывать расширенные хитбоксы

    // ---- ESP ----------------------------------------------------------------
    /** Режим рисования: 0=Box 1=Cube 2=Corners 3=Glow */
    public int     espMode           = 0;
    /** Цвет: 0=Custom 1=ByHP 2=ByType */
    public int     espColorMode      = 2;
    public boolean espShowHealth     = true;
    public boolean espShowName       = true;
    public boolean espShowDistance   = true;
    public boolean espShowLine       = false;
    public boolean espSkeleton       = false;
    public boolean espTargetPlayers  = true;
    public boolean espTargetHostile  = true;
    public boolean espTargetPassive  = false;

    // ---- Legacy (kept for compat) -------------------------------------------
    public int style2     = 0;
    public int extraFlags = 0x0A;
    // -------------------------------------------------------------------------

    public int argb() {
        int r = clamp255((int)(colorR * 255));
        int g = clamp255((int)(colorG * 255));
        int b = clamp255((int)(colorB * 255));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
}

<div align="center">

# 🎨 Cosmetics Mod

**Клиентский Forge-мод для Minecraft 1.16.5**

[![Build](https://github.com/inaga228/Cosmetic-/actions/workflows/build.yml/badge.svg)](https://github.com/inaga228/Cosmetic-/actions/workflows/build.yml)
[![MC Version](https://img.shields.io/badge/Minecraft-1.16.5-brightgreen)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-36%2B-orange)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

Мод добавляет косметику, боевые улучшения и утилиты с красивым GUI-меню.  
Открыть меню: **Right Shift**

</div>

---

## ✨ Возможности

### 🌈 Трейлы
| Трейл | Описание |
|-------|----------|
| Rainbow Trail | Радужный след из частиц за игроком |
| Flame Trail | Огненный след с эффектом пламени |
| Galaxy Trail | Галактический след со звёздными частицами |

### 💫 Аuры (частицы вокруг игрока)
| Аура | Описание |
|------|----------|
| Aura | Орбитальные частицы вокруг тела |
| Snow Aura | Снежинки, кружащиеся вокруг игрока |
| Hearts | Сердечки — аура любви |

### 🎩 Внешний вид
- **China Hat** — кастомная шляпа, рендерится поверх скина
- **Wings / Dragon Wings** — крылья за спиной с анимацией
- **Cape** — плащ с физикой

### 💥 Эффекты
- **Jump Circles** — круги при прыжке
- **Landing Ring** — кольцо при приземлении
- **Hit Effect** — кастомные частицы при ударе (крит, слэш, куб, звезда, сфера и др.)

### 🎯 Боевые функции
| Функция | Описание |
|---------|----------|
| Kill Aura | Автоатака по ближайшим целям |
| Trigger Bot | Автоклик при наведении на игрока (с задержкой) |
| Bow Aimbot | Помощь прицеливания из лука |
| Smooth Aim | Плавное наведение на цели |
| Auto Clicker | Автокликер с настройкой CPS |
| ESP | Подсветка игроков через стены |
| Hitbox | Расширение хитбоксов сущностей |
| Crit | Принудительные криты |
| Strafe | Автострейф вокруг цели |
| Anti Bot | Фильтр ботов |

### 🛠 Утилиты
- **Auto Sprint** — постоянный спринт
- **Auto Jump** — автопрыжок
- **Auto Sneak** — автоприсед
- **Fullbright** — максимальная яркость
- **Auto Totem** — автоматически держит тотем в руке
- **Auto Pot** — автопитьё зелий
- **Auto Gap** — автоеда золотых яблок
- **No Fire Overlay** — убирает огонь с экрана

### 📊 HUD
- **Cosmetics HUD** — панель активных эффектов (верхний левый угол)
- **Target HUD** — информация о цели (здоровье, дистанция)

### ⚙️ Кастомизация анимаций
- **View Model** — смещение и поворот рук
- **Custom Attack Anim** — анимация удара
- **Custom Place Anim** — анимация установки блоков

---

## 📥 Установка

1. Установи [Minecraft Forge 1.16.5](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.5.html)
2. Скачай последний `.jar` из [Releases](https://github.com/inaga228/Cosmetic-/releases)
3. Помести файл в папку `.minecraft/mods/`
4. Запускай игру и нажимай **Right Shift** для открытия меню

---

## 🔨 Сборка из исходников

```bash
git clone https://github.com/inaga228/Cosmetic-.git
cd Cosmetic-
./gradlew build
```

Готовый `.jar` появится в `build/libs/`.

> **CI:** сборка запускается автоматически при каждом пуше через GitHub Actions.

---

## 🏗 Архитектура

```
com.example.cosmetics
├── CosmeticsMod.java          — точка входа, регистрация
├── client/                    — клавиши, состояние, Forge-события
├── particles/                 — регистрация типов частиц и фабрики
│   └── shapes/                — HeartParticle, StarParticle, SlashParticle…
├── trails/                    — TrailTicker, TrailHistory
├── auras/                     — AuraTicker
├── render/                    — HatRenderer, WingsRenderer, TrailRenderer, ESP…
├── combat/                    — TriggerBot, BowAimbot, HitboxExpander
├── utility/                   — CombatHandler, UtilityHandler
├── effects/                   — JumpCircles
├── hud/                       — CosmeticsHud, TargetHud
├── gui/                       — MainMenuScreen, SettingsScreen, HudEditScreen
│   └── widgets/               — Slider, ToggleButton, CycleButton, ColorPicker
├── feature/                   — FeatureType, FeatureSettings
└── config/                    — ConfigManager, ThemeManager, HudPositionManager
```

---

## 📝 Лицензия

[MIT](LICENSE) — делай что хочешь, упомяни автора.

# Changelog

Все изменения по версиям. Формат: `## [версия] - описание`.

---

## [1.1.1] - Фиксы меню, убран ESP, починка No Fire Overlay

### ✨ Добавлено
- **Кастомное главное меню Minecraft** — заменяет стандартное меню автоматически. Тёмная панель с анимацией, плавающими частицами и слайд-анимацией кнопок
- **Вкладка Optim** в меню мода с 5 новыми функциями:
  - `Far View` — дальность прорисовки 32 чанка
  - `FPS Boost` — снимает лимит FPS, отключает VSync
  - `Fast Graphics` — переключает графику в режим Fast
  - `No Clouds` — убирает облака
  - `No Entity Shadow` — убирает тени у сущностей

### 🐛 Исправлено
- **Главное меню** — кнопки (Singleplayer, Multiplayer, Options, Quit) больше не уходят за нижнюю границу панели, все 5 кнопок теперь внутри рамки
- **Настройки фич** — настройки ESP больше не отображались поверх настроек ВСЕХ остальных фич (был висячий блок кода без условия)
- **No Fire Overlay** — теперь действительно убирает огонь с экрана через `setRemainingFireTicks(0)` перед рендером HUD
- **Выход из Cosmetics меню** — больше не вызывает крэш с "сохранение мира"

### 🗑 Удалено
- **ESP** — временно убран из мода, так как работал некорректно (бокс не виден через стены, нет скелета, нет линий, имя отображалось криво). Будет переписан с нуля в следующей версии

---

## [1.1.0] - Кастомное меню, оптимизации

### ✨ Добавлено
- Кастомное главное меню Minecraft с анимацией и частицами
- Вкладка Optim: Far View, FPS Boost, Fast Graphics, No Clouds, No Entity Shadow

### 🐛 Исправлено
- No Fire Overlay — первая попытка исправления

---

## [1.0.1] - Исправления сборки

### 🐛 Исправлено
- `SettingsScreen.java` — ошибка типа: `Float` не мог конвертироваться в `long` для TriggerBot слайдера
- `HitboxExpander.java` — убрано несуществующее в 1.16.5 поле `Attributes.ATTACK_REACH`

---

## [1.0.0] - Первый релиз

### ✨ Добавлено
- **Трейлы**: Rainbow, Flame, Galaxy
- **Ауры**: Aura, Snow Aura, Hearts
- **Внешний вид**: China Hat, Wings, Cape
- **Эффекты**: Jump Circles, Landing Ring, Hit Effect
- **Боевые**: Kill Aura, Trigger Bot, Bow Aimbot, Smooth Aim, Auto Clicker, Hitbox, Crit, Strafe, Anti Bot
- **Утилиты**: Auto Sprint, Auto Jump, Auto Sneak, Fullbright, Auto Totem, Auto Pot, Auto Gap, No Fire Overlay
- **HUD**: Cosmetics HUD, Target HUD
- **Анимации**: View Model, Custom Attack Anim, Custom Place Anim
- Меню открывается через **Right Shift**
- Автосборка через GitHub Actions при каждом пуше

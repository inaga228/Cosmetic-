# Changelog

Все изменения по версиям. Формат: `## [версия] - описание`.

---

## [1.1.0] - Кастомное меню, оптимизации, починка огня

### ✨ Добавлено
- **Кастомное главное меню Minecraft** — тёмная панель с анимацией, плавающими частицами и слайд-анимацией кнопок. Заменяет стандартное меню автоматически
- **Вкладка Optim** в меню мода с 5 новыми функциями:
  - `Far View` — дальность прорисовки 32 чанка
  - `FPS Boost` — снимает лимит FPS, отключает VSync
  - `Fast Graphics` — переключает в режим Fast автоматически
  - `No Clouds` — убирает облака
  - `No Entity Shadow` — убирает тени у сущностей

### 🐛 Исправлено
- **No Fire Overlay** — огонь теперь реально пропадает с экрана. Старый способ (сбрасывать `fireTicks` каждый тик) не работал с рендером; теперь перехватывается сам рендер-ивент `ElementType.FIRE` и отменяется

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
- **Боевые**: Kill Aura, Trigger Bot, Bow Aimbot, Smooth Aim, Auto Clicker, ESP, Hitbox, Crit, Strafe, Anti Bot
- **Утилиты**: Auto Sprint, Auto Jump, Auto Sneak, Fullbright, Auto Totem, Auto Pot, Auto Gap, No Fire Overlay
- **HUD**: Cosmetics HUD, Target HUD
- **Анимации**: View Model, Custom Attack Anim, Custom Place Anim
- Меню открывается через **Right Shift**
- Автосборка через GitHub Actions при каждом пуше

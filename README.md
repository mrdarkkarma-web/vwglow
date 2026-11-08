# VWGlow

Цветное свечение игроков для сервера VanillaWay (Paper 1.21.4+, проверено под 26.x).

- `/glow` — меню выбора: сундук на ПК, форма с кнопками на Bedrock (Floodgate).
- 16 цветов Minecraft, эффекты «Радуга» и «Глиттер».
- Выбор запоминается. Права кончились (ранг истёк) — свечение само выключается.
- В ванише и в режиме наблюдателя свечение не показывается.

## Как работает цвет

В Minecraft цвет свечения = цвет команды (scoreboard team) игрока.

- **С TAB** (по умолчанию): в `plugins/TAB/groups.yml` в конец `tagprefix` добавить `%vwglow_color%`:
  ```yaml
  tagprefix: "%luckperms-prefix%%vwglow_color%"
  ```
  TAB берёт цвет команды из последнего цвета префикса, поэтому ник над головой тоже станет цвета свечения.
- **Без TAB**: плагин сам раскладывает игроков по командам `vwglow_<код>`.

Нужен PlaceholderAPI (для режима TAB).

## Права

| Право | Что даёт |
|---|---|
| `vwglow.use` | открыть `/glow` |
| `vwglow.color.<цвет>` | цвет: `red gold yellow green aqua blue light_purple dark_red dark_green dark_aqua dark_blue dark_purple gray dark_gray white black` |
| `vwglow.color.*` | все 16 цветов |
| `vwglow.effect.rainbow` / `vwglow.effect.glitter` | эффекты |
| `vwglow.effect.*` | оба эффекта |
| `vwglow.admin` | `/glow set <игрок> <режим>`, `/glow reload` |

## Плейсхолдеры

- `%vwglow_color%` — `&b`, `&c` … или пусто
- `%vwglow_name%` — «Голубой» … или пусто
- `%vwglow_mode%` — `aqua` / `rainbow` / `glitter` / `off`

## Сборка

GitHub сам собирает плагин при каждом изменении: вкладка **Actions** → последний запуск → **Artifacts → VWGlow**.
Локально: `gradle build` (Java 21+), файл в `build/libs/`.

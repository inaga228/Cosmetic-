# Cosmetics Mod

Client-side Forge mod for **Minecraft 1.16.5** that adds cosmetic effects:
trails, particle auras, a hat, and a HUD panel.

## Features

- **Trails** — Rainbow, Flame, Galaxy (custom particles)
- **Particles** (auras orbiting / around the player) — Aura, Snow Aura, Hearts
- **Hat** — China Hat rendered through a custom `LayerRenderer` on the player
- **HUD** — smooth-animated gradient panel in the top-left showing the active effects

Open the menu with **Right Shift**.

## Architecture

```
com.example.cosmetics
├── CosmeticsMod.java          mod entry, registration
├── client/                    key bindings, state, forge event subscribers
├── particles/                 particle type registrations + factories
├── trails/                    per-tick trail spawners
├── auras/                     per-tick aura spawners
├── render/                    HatLayer (custom LayerRenderer)
├── gui/                       MainMenuScreen + GuiDraw helpers
└── hud/                       CosmeticsHud overlay
```

## Build

```
./gradlew build
```

Output jar lands in `build/libs/`. CI runs on every push via
`.github/workflows/build.yml`.

## Notes & caveats

- All six particles are registered as `BasicParticleType` via Forge's
  `DeferredRegister` on the mod event bus. Each has a
  `assets/cosmeticsmod/particles/<name>.json` pointing at a texture under
  `assets/cosmeticsmod/textures/particle/<name>.png`.
- Placeholder textures are small 8×8 PNGs — replace them with art you like.
- This is a starter/skeleton. It is written against the 1.16.5 MCP "official"
  mappings. Expect to iterate on minor API details the first time you run it
  (e.g., if `EntityRenderersEvent.AddLayers` isn't available on your exact
  Forge build, move the hat-layer registration into `FMLClientSetupEvent` and
  iterate `Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap()`
  instead).

## License

MIT

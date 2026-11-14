# Street Surfer

A 3D three-lane endless runner game for Android built with **Kotlin**, **OpenGL ES 3.0**, **Jetpack Compose Material 3**, **Room (SQLite)** local persistence, and a **procedural PCM audio synthesizer**.

## Features

- **3-Lane Movement & Physics**: Smooth lateral interpolation across Left, Center, and Right tracks, speed scaling over distance, gravity-driven jumps, slide/roll with 50% hitbox reduction, and mid-air fast-fall slam directly into a roll.
- **Responsive Controls & Input Buffering**: Touch swipes (`Up`, `Down`, `Left`, `Right`), double-tap Hoverboard activation, and keyboard controls (`WASD` / Arrow Keys, `Spacebar` for Hoverboard, `P` / `Esc` for Pause).
- **Stumble & Chaser Mechanic**: Side-clipping an obstacle brings the pursuing inspector guard and dog behind the runner for 8 seconds; running cleanly lets them drop back, while a second hit ends the run.
- **Object-Pooled Procedural Track & Solvability**: Zero-allocation pooled track chunks, obstacles (Low Hurdles, High Barriers, Full Blockers, Stationary & Ramp Trains with walkable roofs, Moving Trains with headlights & horns, Tunnels, Overhead Bridges, Support Pillars), and validated solvability ensuring a passable path at every segment.
- **Power-Ups & Progression**: Jetpack, Super Sneakers, 3-lane Coin Magnet, 2x Multiplier, 30s Hoverboard crash shield, Headstarts, Revive Keys, 3 rotating active Missions (`1x`–`30x` permanent multiplier), and an Upgrade/Cosmetic Shop persisted locally via Room.
- **OpenGL ES 3.0 Sunlit 3D Graphics & Procedural Audio**: Blinn-Phong specular lighting, Fresnel rim highlights, procedural sky dome, 4x MSAA anti-aliasing, pooled 3D particle VFX, synthesized hip-hop music loop, and dynamic rising-pitch coin SFX.

## Architecture

- `app/src/main/java/com/example/game/`: `GameManager`, `PlayerController`, `TrackManager`, `ObstacleSpawner`, `PowerUpManager`, `GameModels`
- `app/src/main/java/com/example/engine/`: `StreetSurferRenderer`, `ShaderProgram`, `MeshBuilder`, `ParticleSystem`
- `app/src/main/java/com/example/progression/` & `data/`: `ProgressionManager`, `StreetCatalog`, `SaveSystem` (Room entities, DAO, database)
- `app/src/main/java/com/example/audio/`: `AudioManager`
- `app/src/main/java/com/example/ui/`: `GameViewModel`, `GameSurfaceView`, `HudOverlay`, `MenuAndOverlays`, `ShopAndMissionsModals`

## Build & Run

1. Open the project in **Android Studio** (or build via Gradle with `ANDROID_SDK_ROOT` configured).
2. Sync Gradle and run the `app` configuration on an Android device/emulator (API 24+, OpenGL ES 3.0).
3. Run unit tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

# Stracciatella — Project Instructions

## General Rules
- Whenever you are changing something in a module, review and update that module's CLAUDE.md so it stays current
- Use your tools in simple ways — always only read 1 file at a time
- Only modify code that needs to be changed. Keep changes as small as possible
- If code for a specific thing already exists, edit it instead of adding new code
- Always fix the logic: if a certain use case is given, assume other same cases are also to be fixed
- Try to avoid doing a text search across all files

## Coordinates
- The minecraft coordinate format: **Y is height**, Z is forward/back. **Z IS NOT THE HEIGHT, Y IS**

## Technology Stack
- **Platform**: Fabric mod loader for Minecraft
- **Mappings**: Official Mojang mappings (not Yarn)
- **Language**: Java (source code), Kotlin (build scripts)
- **Build system**: Gradle with Fabric Loom 1.14+
- **Group**: `net.stracciatella`, **Mod ID**: `stracciatella`
- **Java package root**: `net.stracciatella.<module>`

## Project Structure

```
stracciatella/
├── CLAUDE.md                      # This file — project-wide instructions
├── build.gradle.kts               # Root build script (Loom, dependencies, run configs)
├── settings.gradle.kts            # Module includes, plugin management, repositories
├── gradle.properties              # Version, group, JVM args
├── build-extensions/              # Custom Gradle plugins (stracciatella-root, etc.)
├── loader/                        # Fabric mod loader integration
│   ├── injected/                  # Code injected into Minecraft at load time
│   └── test3module/               # Loader test module
├── modules/                       # All game modules live here
│   ├── build.gradle.kts           # Declares all modules for aggregation
│   ├── core/                      # Core module — shared state, mixins
│   ├── pathfinding/               # Pathfinding module — autonomous movement & parkour
│   ├── testing/                   # Testing framework — in-game integration tests
│   ├── fullscreen/                # Fullscreen module — config and utilities
│   └── anonymous-modlist/         # Anonymous mod list module
├── run/                           # Minecraft runtime directory
│   └── logs/                      # Game logs (latest.log, etc.)
└── mods.versions.toml             # Third-party mod version catalog
```

## Modules

Each module follows the same layout:
```
modules/<name>/
├── CLAUDE.md                      # Module-specific instructions (if exists — check before editing!)
├── build.gradle.kts               # Module build script
└── src/main/java/net/stracciatella/<name>/
    ├── <Name>Module.java          # Entry point (registers commands, ticks, etc.)
    ├── mixin/                     # Minecraft injection points
    └── ...                        # Module-specific packages
```

### Module Overview

| Module | Purpose | Has CLAUDE.md |
|--------|---------|---------------|
| **core** | Shared state management, core mixins | No |
| **pathfinding** | Autonomous player movement, mesh generation, A* pathfinding, parkour | Yes |
| **testing** | In-game integration test framework with annotations and test runner | Yes |
| **fullscreen** | Fullscreen configuration and utilities | No |
| **anonymous-modlist** | Anonymous mod list functionality | No |

### Key Module Files

**Pathfinding** (the most actively developed module):
- `PathWalker.java` — Core movement controller (static, tick-driven, simulates keyboard input)
- `ChunkMeshBuilder.java` — Converts chunks into walkable node graphs
- `MeshPathfinder.java` — A* pathfinding algorithm
- `MeshManager.java` — Per-entity, per-chunk mesh storage and cross-chunk linking
- `PathCommands.java` — All `/path` subcommands
- `PathDisplay.java` — In-game mesh/path rendering
- `PathWalkerTests.java` — In-game integration tests for pathwalking

**Testing**:
- `TestRunner.java` — Singleton test execution engine
- `TestContext.java` — Passed to test methods, call `complete()`/`fail()`
- `MovementController.java` — Static player movement utilities for tests

## Build & Run

```bash
# Build everything
./gradlew build

# Run Minecraft client (light mod set)
./gradlew runClient

# Run automated in-game tests
./gradlew runMinecraftTests

# Build a single module
./gradlew :modules:<name>:build
```

## Logs
- Located in `run/logs/`, mainly use the newest one (`latest.log`)
- Always check the latest logs for context on what was happening during the last execution
- Only read the last 100 lines unless you need to investigate more

## Module Goals

**Pathfinding**: Automate player movements and pathfinding in an as authentic as possible way. The goal is to have the player pathfind and parkour completely automatically, using realistic physics-based controls (simulated keyboard input, not teleportation).

**Testing**: Provide a client-side integration testing framework that runs inside a live Minecraft instance, driven by tick-based execution with annotation-based test discovery.
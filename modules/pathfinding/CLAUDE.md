# Pathfinding Module

Autonomous player movement and pathfinding for Minecraft. Generates navigation meshes from terrain, finds paths with A*, and walks them with realistic physics-based controls.

## Architecture

```
net.stracciatella.pathfinding
├── PathfindingModule.java            # Entry point. Loads configs, registers commands/ticks/tests
├── ChunkCoordinate.java              # 2D chunk coordinate wrapper (x, z)
├── commands/
│   └── PathCommands.java             # All /path subcommands (find, walk, config, calibrate, etc.)
├── display/
│   └── PathDisplay.java              # In-game mesh/path rendering (custom no-depth-test lines)
├── logic/
│   ├── PathWalker.java               # Core autonomous movement controller (static, tick-driven)
│   ├── ChunkMeshBuilder.java         # Converts chunks into walkable node graphs
│   ├── MeshManager.java              # Stores meshes per entity per chunk, handles cross-chunk linking
│   ├── MeshPathfinder.java           # A* algorithm with Euclidean heuristic
│   └── mesh/
│       ├── Mesh.java                 # HashMap<BlockPos, MeshNode> container for one chunk
│       ├── MeshNode.java             # Graph vertex: x, y, z + List<Neighbor>
│       ├── Neighbor.java             # Weighted edge: target node + cost
│       └── IMeshProvider.java        # Interface for mesh sources
├── mixin/
│   └── LevelChunkMixin.java          # Triggers mesh generation on chunk load
└── test/
    └── PathWalkerTests.java          # In-game tests: straight-line + L-shaped path walking
```

## Key data flow

1. **Mesh generation**: Chunk loads → `LevelChunkMixin` → `MeshManager.generateMesh()` → `ChunkMeshBuilder` scans blocks, creates MeshNodes where player can stand (solid block + 2 air above), connects neighbors via reachability checks → stores in `MeshManager.meshes` → reconnects border nodes with adjacent chunks
2. **Pathfinding**: `/path find` → `MeshPathfinder.findPath(start, end)` → A* search → returns `List<MeshNode>`
3. **Path walking**: `PathWalker.start(path)` → each tick: get target node, calculate jump decision, update aim/rotation, apply movement keys → when node reached, advance index → when path done, `stop()`

## PathWalker — the core component

PathWalker is entirely **static**. It simulates keyboard input (forward, sprint, jump keys) — it does NOT teleport or set position directly.

### Important concepts

- **`gap`** = `Math.max(abs(target.X - playerX), abs(target.Z - playerZ))` — includes both endpoints. gap=5 means 4 air blocks ("4-block jump" in parkour terms)
- **`longRangeJump`** = gap ≥ 3 || distance ≥ 2.5. Uses collision-based edge detection instead of simulation
- **Node arrival**: sphere check (distance < 0.18) OR box check within block bounds with 0.15 margin
- **Target offset**: random X/Z offset (0.05–0.25) added for natural-looking movement, suppressed for long jumps

### Jump decision system (`shouldJumpNow()`)

Returns a `JumpDecision` record: `(jump, gap, holdBeforeJump, reason)`

Decision paths in priority order:
1. **Same block** (gap=0): jump if target ≥0.5 blocks higher
2. **No direction**: skip if stepX==0 && stepZ==0
3. **Landing validation**: check forward air and landing block solidity
4. **Drop check**: no jump for small drops (gap ≤ 1, target lower)
5. **Step-up** (gap ≤ 1): jump if target higher or block in front, only when close
6. **Long-range** (gap ≥ 3 or distance ≥ 2.5): collision-based edge detection, preserves sprint
7. **Simulation** (short-range, gap < 3): physics simulation to predict landing
8. **Edge-distance**: fractional block position checks for when to fire/hold
9. **Fallback**: block-edge position check

### Config system

- Stored in `pathwalker.json`, loaded/saved via `PathWalker.loadConfig()`/`saveConfig()`
- `PathWalker.CONFIG` is the public static Config object
- All parameters tunable via `/path walkconfig` commands
- Key parameter groups: turn/aim, edge jump thresholds, jump simulation, physics, off-course detection

### Learning and calibration

- **Learning** (`/path walklearn on`): records manual jump thresholds while pathwalking is inactive, applies learned edge ranges to config
- **Calibration** (`/path walkcalibrate start`): measures actual physics (gravity, drag, jump velocity, acceleration) from gameplay, updates config physics values

### Debug output

PathWalker has extensive debug logging. **Always read the logs when editing PathWalker.** Enable with `/path walkdebug on`. Outputs: distance, angle, facing, jump reason, fall diagnostics. Updates every 200ms.

## ChunkMeshBuilder — mesh generation

### Node creation rules
- Solid block at Y, air at Y+1, air at Y+2 → walkable node at (X, Y, Z)
- Search limits: horizontal=5, up=3, down=5, max safe drop=3, diagonal=5

### Neighbor connection
- Brute-force search within radius for each node
- `isBlockReachable()`: validates height constraints, diagonal limits, line-of-sight (Bresenham)
- `movementCost()`: gap=1→10, gap=2→22, gap=3→40, gap=4→65, gap>4→100, plus height/diagonal penalties

### Cross-chunk connectivity
- `MeshManager.connectAdjacentMeshes()` links border nodes when neighbor chunks exist
- `reconnectBorderNodes()` rebuilds edges for nodes on chunk boundaries

## Commands (`/path`)

| Command | Description |
|---------|-------------|
| `path pos start\|end` | Set start/end positions |
| `path find` | Calculate and display path |
| `path walk` | Walk the calculated path |
| `path walk stop` | Stop pathwalking |
| `path walkdebug on\|off` | Toggle debug logging |
| `path walklearn on\|off` | Toggle jump learning |
| `path walkcalibrate start\|stop\|status` | Physics calibration |
| `path walkconfig menu` | Interactive config menu |
| `path walkconfig <param> <values>` | Tune individual parameters |
| `generateMesh` | Generate mesh for current chunk |
| `displayConnections` | Toggle mesh visualization |
| `path connections all\|path\|none` | Connection display mode |

## PathDisplay — rendering

- Custom `RenderType` with no depth test (visible through blocks)
- Yellow outlines for nodes, colored lines for connections
- Orange/yellow for highlighted path, light blue for others
- Config stored in `pathdisplay.json`
- Access widener used for `RenderPipelines.DEBUG_LINE_STRIP` and `LINES_SNIPPET`

## Build configuration

- Dependencies: loader (compileOnly), testing module (compileOnly), sodium (modCompileOnly, optional)
- Mixin config: `pathfinding.mixins.json` (client: LevelChunkMixin)
- Access widener: `pathfinding.accesswidener`

## Minecraft physics reference

- Y is height, Z is forward/back
- Post-drag sprint speed: ~0.153 b/t, pre-drag: ~0.28 b/t
- Ground friction factor: 0.546
- Sprint jump boost: +0.2 horizontal velocity (pre-drag)
- Max sprint jump: ~4.5 blocks flat, ~5 blocks with 1-block drop
- Player standing on block at y=-55 has feet at y=-54
- `feetToTargetDy = target.getY() - player.getY()`; negative = target block is lower

## Testing

- In-game tests in `test/PathWalkerTests.java` (registered by PathfindingModule)
- JUnit tests in `src/test/.../MeshPathfinderTest.java` (A* algorithm verification)
- Run in-game tests: `/stracciatella-test` after joining a world

# Camera Module — Design Decisions

## Instance-based CameraController vs static utility

**Decision**: CameraController is instance-based (non-static).

### Alternatives
| Approach | Pros | Cons |
|----------|------|------|
| Instance-based | Multiple independent camera controllers, testable, no shared state | Requires field to hold instance |
| Static (like PathWalker) | Simple access, no instantiation | Only one camera at a time, hard to test, couples consumers |

Chose instance-based because the goal is to support multiple automation systems (pathfinding, combat, mining) each with independent camera state.

## Camera physics hardcoded as constants

**Decision**: Spring physics parameters (`TURN_ACCEL=0.8`, `TURN_FRICTION=0.8`) are hardcoded constants in `CameraController`, not configurable at runtime.

### Alternatives
| Approach | Pros | Cons |
|----------|------|------|
| Hardcoded constants | Simple, no config files, values are proven stable | Requires code change to tune |
| Config file (camera.json) | Tunable without recompile | Extra complexity, another config to manage |
| Passed from consumer config | Flexible per-consumer | Couples camera module to consumer's config |

Chose hardcoded constants because the values are well-tuned from extensive testing. Runtime tuning via config commands was removed to simplify the system.

## isFacingTarget tolerance as parameter vs config

**Decision**: `AngleUtil.isFacingTarget` takes tolerance as a parameter instead of reading from a config.

### Alternatives
| Approach | Pros | Cons |
|----------|------|------|
| Tolerance as parameter | Flexible, no coupling to any config, reusable | Caller must pass value |
| Read from CameraController config | Encapsulated | Ties utility to controller instance, less reusable |
| Read from global config | Simple | Couples to specific config, unusable by other modules |

Chose parameter approach because different contexts need different tolerances (gap=2 uses 18 degrees, gap>=3 uses 36 degrees, step-ups use 45 degrees).

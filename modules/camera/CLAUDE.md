# Camera Module

Human-like camera movement for Minecraft automation. Provides smooth yaw/pitch control that avoids robotic snapping to targets.

## Architecture

```
net.stracciatella.camera
├── CameraModule.java       # Entry point (empty — pure utility module)
├── CameraController.java   # Instance-based yaw/pitch smoothing (spring physics + exponential smoothing)
└── AngleUtil.java           # Static angle utilities (wrapDegrees, isFacingTarget, computeDesiredPitch)
```

## Key Classes

### CameraController
- **Yaw**: Spring-damper physics (hardcoded `TURN_ACCEL=0.8`, `TURN_FRICTION=0.8`).
- **Pitch**: Exponential smoothing (15% per tick), snaps within 0.5 degrees.
- Instance-based — each consumer creates its own controller.
- `initialize(yaw, pitch)` resets state. `snapYaw(yaw)` for instant direction changes.

### AngleUtil
- `wrapDegrees(float)` — normalize to [-180, 180)
- `isFacingTarget(float currentYaw, float targetYaw, float toleranceDeg)` — tolerance-based facing check
- `computeDesiredPitch(double dy, double distance)` — pitch from vertical delta, clamped to [-15, 15] degrees

## Dependencies

- `loader` (compileOnly) — for the `Module` interface

## Consumers

- **Pathfinding module** (`PathWalker.java`) — uses `CameraController` for smooth camera during path walking

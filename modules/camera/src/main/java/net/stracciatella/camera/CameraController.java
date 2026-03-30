package net.stracciatella.camera;

/**
 * Controls camera yaw and pitch with human-like smoothing.
 *
 * <p>Yaw uses spring-damper physics (critically damped for responsive, non-oscillating turns).
 * Pitch uses exponential smoothing for gentle vertical tracking.
 *
 * <p>Instance-based so different systems can maintain independent camera state.
 */
public class CameraController {

    // Spring physics for yaw rotation
    private static final float TURN_ACCEL = 0.8f;
    private static final float TURN_FRICTION = 0.8f;

    // Facing tolerance: how close yaw must be to target before a jump fires
    public static final float JUMP_FACING_TOLERANCE_DEG = 18.0f;
    // Extra tolerance added for gap >= 3 jumps (total = JUMP_FACING_TOLERANCE + EXTRA)
    public static final float JUMP_FACING_EXTRA_GAP_DEG = 18.0f;

    // Turn behavior thresholds
    public static final float WALK_TURN_THRESHOLD_DEG = 25.0f;
    public static final float SHARP_TURN_DEG = 60.0f;
    public static final float TURN_STOP_THRESHOLD_DEG = 12.0f;
    public static final float WALK_TURN_MAX_DEG = 75.0f;

    // Random yaw offset range during jump preparation
    public static final float JUMP_AIM_YAW_MIN_DEG = 1.5f;
    public static final float JUMP_AIM_YAW_MAX_DEG = 4.0f;

    private float yaw;
    private float yawVelocity;
    private float yawAccel;
    private float pitch;

    /**
     * Sets the initial yaw and pitch, zeroing all velocity and acceleration.
     * Call this when starting camera control (e.g. at the beginning of a path walk).
     */
    public void initialize(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.yawVelocity = 0.0f;
        this.yawAccel = 0.0f;
    }

    /**
     * Updates yaw toward the target using spring-damper physics.
     * Returns the new yaw value.
     */
    public float updateYaw(float targetYaw) {
        float delta = AngleUtil.wrapDegrees(targetYaw - yaw);
        yawAccel = delta * TURN_ACCEL - yawVelocity * TURN_FRICTION;
        yawVelocity += yawAccel;
        yaw += yawVelocity;
        return yaw;
    }

    /**
     * Updates pitch toward the target using exponential smoothing.
     * Snaps to target when within 0.5 degrees to avoid micro-adjustments.
     * Returns the new pitch value.
     */
    public float updatePitch(float targetPitch) {
        float delta = targetPitch - pitch;
        if (Math.abs(delta) < 0.5f) {
            pitch = targetPitch;
            return pitch;
        }
        // Move a fraction of the distance each tick (exponential smoothing)
        pitch += delta * 0.15f;
        // Clamp to valid Minecraft range
        pitch = Math.max(-90.0f, Math.min(90.0f, pitch));
        return pitch;
    }

    /**
     * Instantly sets yaw to the given value and zeroes velocity/acceleration.
     * Used for snapping camera direction (e.g. during landing brake).
     */
    public void snapYaw(float yaw) {
        this.yaw = yaw;
        this.yawVelocity = 0;
        this.yawAccel = 0;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYawVelocity() {
        return yawVelocity;
    }
}

package net.stracciatella.camera;

/**
 * Static utility methods for angle calculations used in camera control.
 */
public final class AngleUtil {

    private AngleUtil() {
    }

    /**
     * Normalizes an angle to the range [-180, 180).
     */
    public static float wrapDegrees(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    /**
     * Checks whether {@code currentYaw} is within {@code toleranceDeg} of {@code targetYaw}.
     */
    public static boolean isFacingTarget(float currentYaw, float targetYaw, float toleranceDeg) {
        float delta = Math.abs(wrapDegrees(targetYaw - currentYaw));
        return delta <= toleranceDeg;
    }

    /**
     * Computes a desired pitch angle based on vertical distance and horizontal distance.
     * Returns 0 for small differences, clamped to [-15, 15] degrees for larger ones.
     */
    public static float computeDesiredPitch(double dy, double distance) {
        // Keep pitch more neutral for human-like movement
        // Only look significantly up/down for large vertical differences
        if (distance > 0.1 && Math.abs(dy) > 1.0) {
            // dy = targetY - eyeY, so positive dy means look up (negative pitch in MC)
            float pitch = (float) -Math.toDegrees(Math.atan(dy / distance));
            // Clamp pitch to reasonable range for walking
            return Math.max(-15.0f, Math.min(15.0f, pitch));
        }
        return 0f;
    }
}

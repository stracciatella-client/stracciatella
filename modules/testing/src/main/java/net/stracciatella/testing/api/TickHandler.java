package net.stracciatella.testing.api;

/**
 * Interface for tests that need per-tick updates.
 * Implement this on your test suite class to receive tick callbacks
 * while your test is running.
 */
@FunctionalInterface
public interface TickHandler {
    /**
     * Called every client tick while the associated test is active.
     *
     * @param ctx the test context
     */
    void onTick(TestContext ctx);
}

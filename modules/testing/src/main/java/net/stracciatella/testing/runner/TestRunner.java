package net.stracciatella.testing.runner;

import net.minecraft.client.Minecraft;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.api.TickHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger("TestRunner");
    private static final TestRunner INSTANCE = new TestRunner();

    private final List<RegisteredTest> pendingTests = new ArrayList<>();
    private final List<TestResult> results = new CopyOnWriteArrayList<>();
    private RegisteredTest currentTest;
    private TestContext currentContext;
    private int currentTickCount;
    private boolean running;
    private boolean finished;
    private boolean autoRunTriggered;
    private Runnable onAllComplete;

    private TestRunner() {
    }

    public static TestRunner instance() {
        return INSTANCE;
    }

    /**
     * Register a test suite class. All methods annotated with {@link MinecraftTest} will be discovered.
     */
    public void registerSuite(Class<?> suiteClass) {
        TestSuite suiteAnnotation = suiteClass.getAnnotation(TestSuite.class);
        String suiteName;
        if (suiteAnnotation != null && !suiteAnnotation.name().isEmpty()) {
            suiteName = suiteAnnotation.name();
        } else {
            suiteName = suiteClass.getSimpleName();
        }

        Object instance;
        try {
            instance = suiteClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate test suite: {}", suiteClass.getName(), e);
            return;
        }

        for (Method method : suiteClass.getDeclaredMethods()) {
            MinecraftTest annotation = method.getAnnotation(MinecraftTest.class);
            if (annotation == null) continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || params[0] != TestContext.class) {
                LOGGER.warn("Skipping test method {} - must accept exactly one TestContext parameter", method.getName());
                continue;
            }

            method.setAccessible(true);
            pendingTests.add(new RegisteredTest(suiteName, instance, method, annotation));
        }

        LOGGER.info("Registered test suite '{}' with {} tests", suiteName,
                pendingTests.stream().filter(t -> t.suiteName().equals(suiteName)).count());
    }

    /**
     * Start running all registered tests sequentially.
     */
    public void start(Runnable onAllComplete) {
        this.onAllComplete = onAllComplete;
        pendingTests.sort(Comparator.comparingInt(t -> t.annotation().order()));
        running = true;
        finished = false;
        results.clear();
        LOGGER.info("Starting {} tests", pendingTests.size());
        advanceToNext();
    }

    /**
     * Called every client tick by the mixin. Drives the test execution.
     */
    public void onClientTick() {
        // Auto-run when player joins world if system property is set
        if (!autoRunTriggered && !running && "true".equals(System.getProperty("stracciatella.testing.autorun"))) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !pendingTests.isEmpty()) {
                autoRunTriggered = true;
                LOGGER.info("Auto-run triggered by system property");
                start(() -> {
                    boolean allPassed = results.stream().allMatch(r -> r.status() == TestResult.Status.PASSED);
                    if (allPassed) {
                        LOGGER.info("All tests PASSED — shutting down");
                    } else {
                        LOGGER.error("Some tests FAILED — shutting down");
                    }
                    Minecraft.getInstance().stop();
                });
            }
        }

        if (!running || currentTest == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        currentTickCount++;

        // Check timeout
        int timeout = currentTest.annotation().timeoutTicks();
        if (timeout > 0 && currentTickCount >= timeout) {
            recordResult(TestResult.Status.TIMED_OUT, "Timed out after " + timeout + " ticks");
            advanceToNext();
            return;
        }

        // Tick the suite if it implements TickHandler
        if (currentTest.suiteInstance() instanceof TickHandler handler && currentContext != null && !currentContext.isFinished()) {
            try {
                handler.onTick(currentContext);
            } catch (Exception e) {
                recordResult(TestResult.Status.ERROR, "TickHandler error: " + e.getMessage());
                advanceToNext();
            }
        }

        // Check if the test completed itself via context
        if (currentContext != null && currentContext.isFinished()) {
            advanceToNext();
        }
    }

    private void advanceToNext() {
        if (pendingTests.isEmpty()) {
            running = false;
            finished = true;
            printReport();
            if (onAllComplete != null) onAllComplete.run();
            return;
        }

        currentTest = pendingTests.remove(0);
        currentTickCount = 0;

        Minecraft mc = Minecraft.getInstance();
        currentContext = new TestContext(mc, () -> {
            recordResult(TestResult.Status.PASSED, "");
        }, reason -> {
            recordResult(TestResult.Status.FAILED, reason);
        });

        LOGGER.info("Running: [{}] {}", currentTest.suiteName(), currentTest.displayName());

        try {
            currentTest.method().invoke(currentTest.suiteInstance(), currentContext);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            recordResult(TestResult.Status.ERROR, cause.getClass().getSimpleName() + ": " + cause.getMessage());
            advanceToNext();
        }
    }

    private void recordResult(TestResult.Status status, String message) {
        results.add(new TestResult(currentTest.suiteName(), currentTest.displayName(), status, message, currentTickCount));
    }

    private void printReport() {
        LOGGER.info("========== TEST RESULTS ==========");
        int passed = 0, failed = 0;
        for (TestResult result : results) {
            String icon = switch (result.status()) {
                case PASSED -> "PASS";
                case FAILED -> "FAIL";
                case TIMED_OUT -> "TIMEOUT";
                case ERROR -> "ERROR";
            };
            String line = String.format("[%s] %s > %s (%d ticks)", icon, result.suiteName(), result.testName(), result.durationTicks());
            if (!result.message().isEmpty()) {
                line += " - " + result.message();
            }
            if (result.status() == TestResult.Status.PASSED) {
                LOGGER.info(line);
                passed++;
            } else {
                LOGGER.error(line);
                failed++;
            }
        }
        LOGGER.info("==================================");
        LOGGER.info("Total: {} | Passed: {} | Failed: {}", passed + failed, passed, failed);
        LOGGER.info("==================================");
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public List<TestResult> results() {
        return List.copyOf(results);
    }
}

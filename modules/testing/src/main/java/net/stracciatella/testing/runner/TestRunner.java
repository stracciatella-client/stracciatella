package net.stracciatella.testing.runner;

import net.minecraft.client.Minecraft;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
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

    private final List<RegisteredTest> registeredTests = new ArrayList<>();
    private final List<TestResult> results = new CopyOnWriteArrayList<>();
    private volatile boolean running;
    private volatile boolean autoRunTriggered;
    private TestContext activeContext;

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
            int repeat = Math.max(1, annotation.repeat());
            for (int i = 1; i <= repeat; i++) {
                registeredTests.add(new RegisteredTest(suiteName, instance, method, annotation, i));
            }
        }

        LOGGER.info("Registered test suite '{}' with {} tests", suiteName,
                registeredTests.stream().filter(t -> t.suiteName().equals(suiteName)).count());
    }

    /**
     * Start running all registered tests on a dedicated thread.
     *
     * @param onComplete called on the test thread when all tests finish
     */
    public void start(Runnable onComplete) {
        if (running) return;
        running = true;

        TestContext ctx = new TestContext();
        activeContext = ctx;

        Thread testThread = new Thread(() -> {
            try {
                runAll(ctx);
            } finally {
                running = false;
                activeContext = null;
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }, "Test Runner");
        testThread.setDaemon(true);
        testThread.start();
    }

    /**
     * Called every client tick by the mixin. Signals the test thread and handles auto-run.
     */
    public void onClientTick() {
        // Auto-run when player joins world if system property is set
        if (!autoRunTriggered && !running && "true".equals(System.getProperty("stracciatella.testing.autorun"))) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !registeredTests.isEmpty()) {
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

        // Signal the test thread that a tick has completed
        if (activeContext != null) {
            activeContext.onClientTick();
        }
    }

    /**
     * Run all registered tests synchronously on the calling thread.
     */
    private void runAll(TestContext ctx) {
        List<RegisteredTest> tests = new ArrayList<>(registeredTests);
        tests.sort(Comparator.comparingInt(t -> t.annotation().order()));
        results.clear();

        // Wait for player to be in world
        ctx.waitFor(mc -> mc.player != null, 6000);

        // Release mouse grab so cursor is free during tests
        ctx.runOnClient(mc -> mc.mouseHandler.releaseMouse());

        LOGGER.info("Starting {} tests", tests.size());

        for (RegisteredTest test : tests) {
            LOGGER.info("Running: [{}] {}", test.suiteName(), test.displayName());

            int timeoutTicks = test.annotation().timeoutTicks();
            ctx.setTimeoutTicks(timeoutTicks);

            long startTime = System.currentTimeMillis();
            try {
                test.method().invoke(test.suiteInstance(), ctx);
                long elapsed = System.currentTimeMillis() - startTime;
                results.add(new TestResult(test.suiteName(), test.displayName(),
                        TestResult.Status.PASSED, "", elapsed));
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();

                TestResult.Status status;
                if (message.contains("Timed out") || message.contains("timed out")) {
                    status = TestResult.Status.TIMED_OUT;
                } else if (cause instanceof AssertionError) {
                    status = TestResult.Status.FAILED;
                } else {
                    status = TestResult.Status.ERROR;
                }

                results.add(new TestResult(test.suiteName(), test.displayName(),
                        status, message, elapsed));
            }
        }

        printReport();
    }

    private void printReport() {
        LOGGER.info("========== TEST RESULTS ==========");
        int passed = 0;
        int failed = 0;
        for (TestResult result : results) {
            String icon = switch (result.status()) {
                case PASSED -> "PASS";
                case FAILED -> "FAIL";
                case TIMED_OUT -> "TIMEOUT";
                case ERROR -> "ERROR";
            };
            String line = String.format("[%s] %s > %s (%dms)", icon, result.suiteName(), result.testName(), result.durationMs());
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

    public List<TestResult> results() {
        return List.copyOf(results);
    }
}

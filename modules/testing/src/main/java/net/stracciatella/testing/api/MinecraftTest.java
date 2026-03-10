package net.stracciatella.testing.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a minecraft integration test.
 * Methods must accept a single {@link TestContext} parameter and return void.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MinecraftTest {
    /**
     * Display name for the test. Defaults to the method name.
     */
    String name() default "";

    /**
     * Timeout in ticks before the test is considered failed. 0 = no timeout.
     */
    int timeoutTicks() default 600;

    /**
     * Order of execution. Lower values run first.
     */
    int order() default 0;

    /**
     * Number of times to repeat this test. Each iteration runs as a separate test.
     */
    int repeat() default 1;
}

package net.stracciatella.testing.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a test suite containing {@link MinecraftTest} methods.
 * Test suites are discovered and registered by the test runner.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestSuite {
    /**
     * Display name for this test suite. Defaults to the class simple name.
     */
    String name() default "";
}

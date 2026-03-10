package net.stracciatella.testing.runner;

import net.stracciatella.testing.api.MinecraftTest;

import java.lang.reflect.Method;

record RegisteredTest(String suiteName, Object suiteInstance, Method method, MinecraftTest annotation, int iteration) {
    String displayName() {
        String name = annotation.name();
        String base = name.isEmpty() ? method.getName() : name;
        if (annotation.repeat() > 1) {
            return base + " [" + iteration + "/" + annotation.repeat() + "]";
        }
        return base;
    }
}

package net.stracciatella.testing.runner;

import net.stracciatella.testing.api.MinecraftTest;

import java.lang.reflect.Method;

record RegisteredTest(String suiteName, Object suiteInstance, Method method, MinecraftTest annotation) {
    String displayName() {
        String name = annotation.name();
        return name.isEmpty() ? method.getName() : name;
    }
}

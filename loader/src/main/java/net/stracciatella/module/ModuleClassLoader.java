package net.stracciatella.module;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

public interface ModuleClassLoader {
    Collection<ModuleClassLoader> dependencyLoaders();

    @Nullable Class<?> loadClassFromParent(String name);
}

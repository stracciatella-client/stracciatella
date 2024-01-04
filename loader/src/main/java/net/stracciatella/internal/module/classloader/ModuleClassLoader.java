package net.stracciatella.internal.module.classloader;

import java.net.URL;
import java.util.Collection;

import org.jetbrains.annotations.Nullable;

public interface ModuleClassLoader {
    Collection<ModuleClassLoader> dependencyLoaders();

    @Nullable Class<?> loadClassFromParent(String name);

    @Nullable URL getResourceFromParent(String name);
}

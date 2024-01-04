package net.stracciatella.module.dependency;

import java.net.MalformedURLException;
import java.net.URL;

import net.stracciatella.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * This is a {@link Module} that is a dependency for another {@link Module}.
 */
public record ModuleModuleDependency(@NotNull String name) implements ModuleDependency {
    @Override
    public URL resolve() throws MalformedURLException {
        return null;
    }
}

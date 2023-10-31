package net.stracciatella.module;

import java.net.MalformedURLException;
import java.net.URL;

import org.jetbrains.annotations.NotNull;

/**
 * This is a maven dependency for a {@link Module}.
 */
public record MavenModuleDependency(@NotNull Repository repository, @NotNull String group, @NotNull String name, @NotNull String version) implements ModuleDependency {
    @Override
    public URL resolve() throws MalformedURLException {
        return repository.resolve(this);
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", group, name, version);
    }
}

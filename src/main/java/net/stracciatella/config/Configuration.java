package net.stracciatella.config;

import java.io.IOException;
import java.nio.file.Path;

public interface Configuration {

    /**
     * Creates an in-memory configuration. This has no saving capabilities.
     *
     * @return a new in-memory configuration
     */
    static Configuration newConfiguration() {
        return new GsonConfiguration(null, null, null);
    }

    /**
     * Creates a path-backed configuration. This will auto-save to the given path
     *
     * @param loader the {@link ConfigLoader} to use
     * @param path   the path to load the config from
     * @return a path-backed configuration
     */
    static Configuration newConfiguration(ConfigLoader loader, Path path) throws IOException {
        return loader.loadConfig(path);
    }

    /**
     * Creates a path-backed gson configuration.
     *
     * @param path the path to load the config from
     * @return a path-backed gson configuration
     * @see #newConfiguration(ConfigLoader, Path)
     */
    static Configuration newGsonConfiguration(Path path) throws IOException {
        return newConfiguration(ConfigLoader.GSON, path);
    }

    <T> T value(ConfigurationAttribute<T> attribute);

    <T> void value(ConfigurationAttribute<T> attribute, T value);

}

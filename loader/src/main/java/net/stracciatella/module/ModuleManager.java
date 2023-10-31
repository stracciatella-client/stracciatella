package net.stracciatella.module;

import java.nio.file.Path;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModuleManager {

    @NotNull Collection<? extends ModuleEntry> modules();

    @Nullable ModuleEntry module(@NotNull String moduleId);

    @NotNull ModuleEntry module(@NotNull Module module);

    /**
     * Load a module from a jar.
     *
     * @param file the file where the jar lies
     * @throws Throwable if an error occurs
     */
    @NotNull ModuleEntry load(@NotNull Path file) throws Throwable;

    void reload(@NotNull ModuleEntry module) throws Throwable;

    void unload(@NotNull ModuleEntry module) throws Throwable;

}

package net.stracciatella.module;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleModuleEntry implements ModuleEntry {
    private final ModuleConfiguration moduleConfiguration;
    private final FileSystem fileSystem;
    private final Path file;
    private final Set<SimpleModuleEntry> dependencies = new HashSet<>();
    private final Set<SimpleModuleEntry> dependants = new HashSet<>();
    private final Logger logger;

    private volatile SimpleModuleClassLoader classLoader;
    private volatile Module module;
    private volatile Module.LifeCycle lifeCycle;

    public SimpleModuleEntry(ModuleConfiguration moduleConfiguration, FileSystem fileSystem, Path file) {
        this.moduleConfiguration = moduleConfiguration;
        this.fileSystem = fileSystem;
        this.file = file;
        this.logger = LoggerFactory.getLogger(moduleConfiguration.name());
    }

    @Override
    public @NotNull ModuleConfiguration moduleConfiguration() {
        return moduleConfiguration;
    }

    @Override
    public Module module() {
        return module;
    }

    void module(Module module) {
        this.module = module;
    }

    @Override
    public Module.@NotNull LifeCycle lifeCycle() {
        return lifeCycle;
    }

    Module.LifeCycle lifeCycle(Module.LifeCycle lifeCycle) {
        logger.debug("Changed LifeCycle to " + lifeCycle);
        var lastLifeCycle = this.lifeCycle;
        this.lifeCycle = lifeCycle;
        return lastLifeCycle;
    }

    @Override
    public @NotNull Logger logger() {
        return logger;
    }

    @ApiStatus.Internal
    public SimpleModuleClassLoader classLoader() {
        return classLoader;
    }

    void classLoader(SimpleModuleClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public FileSystem fileSystem() {
        return fileSystem;
    }

    public synchronized Set<SimpleModuleEntry> dependencies() {
        return dependencies;
    }

    public synchronized Set<SimpleModuleEntry> dependants() {
        return dependants;
    }

    public @NotNull Path file() {
        return file;
    }
}

package net.stracciatella.module;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;

public interface ModuleEntry {

    @NotNull ModuleConfiguration moduleConfiguration();

    @UnknownNullability
    Module module();

    @NotNull Module.LifeCycle lifeCycle();

    @NotNull Logger logger();

    @NotNull Path file();

}

package net.stracciatella.internal.module;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class CommandLineModuleClasspath {
    private final Set<String> paths = new HashSet<>();

    private CommandLineModuleClasspath(Collection<String> paths) {
        this.paths.addAll(paths);
    }

    public static CommandLineModuleClasspath fromClasspath(String classpath) {
        return new CommandLineModuleClasspath(classpath == null ? Collections.emptyList() : Arrays.asList(classpath.split(Pattern.quote(File.pathSeparator))));
    }

    public static CommandLineModuleClasspath fromClasspathFile(String file) throws IOException {
        var path = Path.of(file);
        if (!Files.exists(path)) return fromClasspath(null);
        var data = Files.readString(path);
        return fromClasspath(data);
    }

    public Set<String> paths() {
        return Collections.unmodifiableSet(paths);
    }
}

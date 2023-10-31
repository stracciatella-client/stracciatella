package net.stracciatella.module;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class LibraryStorage {

    private final Path directory;
    private final Path mavenDirectory;

    public LibraryStorage() {
        this(findDirectory());
    }

    public LibraryStorage(Path directory) {
        this.directory = directory;
        this.mavenDirectory = this.directory.resolve("maven");
    }

    private static Path findDirectory() {
        var storagePathString = System.getProperty("stracciatellaLibraryStorage");
        if (storagePathString == null) storagePathString = "libraries";
        return Path.of(storagePathString);
    }

    public Path directory() {
        return directory;
    }

    public Path mavenDirectory() {
        return mavenDirectory;
    }

    public void store(MavenModuleDependency dependency, InputStream in) throws IOException {
        var path = path(dependency);
        var data = in.readAllBytes();
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }

    public Path path(MavenModuleDependency dependency) {
        var group = dependency.group();
        var name = dependency.name();
        var version = dependency.version();
        return this.mavenDirectory.resolve(group).resolve(name).resolve(version).resolve(name + "-" + version + ".jar");
    }

    public boolean contains(MavenModuleDependency dependency) {
        return Files.exists(path(dependency));
    }
}

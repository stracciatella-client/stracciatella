package net.stracciatella.module.dependency;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public record MavenRepository(String repositoryUrl) implements Repository {

    public static final MavenRepository CENTRAL = new MavenRepository("https://repo1.maven.org/maven2/");

    private static final String FORMAT = "%s%s/%s/%s/%s-%s.jar";

    public MavenRepository {
        if (!repositoryUrl.endsWith("/")) repositoryUrl = repositoryUrl + "/";
    }

    @Override
    public URL resolve(ModuleDependency dependency) throws MalformedURLException {
        if (!(dependency instanceof MavenModuleDependency maven)) throw new IllegalArgumentException("Unable to resolve " + dependency.getClass() + " for a maven repository");
        return URI.create(String.format(FORMAT, repositoryUrl, maven.group().replace('.', '/'), maven.name(), maven.version(), maven.name(), maven.version())).toURL();
    }
}

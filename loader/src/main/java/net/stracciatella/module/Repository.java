package net.stracciatella.module;

import java.net.MalformedURLException;
import java.net.URL;

public interface Repository {
    URL resolve(ModuleDependency dependency) throws MalformedURLException;
}

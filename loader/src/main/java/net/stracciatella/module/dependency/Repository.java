package net.stracciatella.module.dependency;

import java.net.MalformedURLException;
import java.net.URL;

import net.stracciatella.module.dependency.ModuleDependency;

public interface Repository {
    URL resolve(ModuleDependency dependency) throws MalformedURLException;
}

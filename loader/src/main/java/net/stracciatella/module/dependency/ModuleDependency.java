package net.stracciatella.module.dependency;

import java.net.MalformedURLException;
import java.net.URL;

import net.stracciatella.module.Module;

/**
 * A dependency for a {@link Module}.
 */
public interface ModuleDependency {
    URL resolve() throws MalformedURLException;
}

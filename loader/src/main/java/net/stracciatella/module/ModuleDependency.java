package net.stracciatella.module;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A dependency for a {@link Module}.
 */
public interface ModuleDependency {
    URL resolve() throws MalformedURLException;
}

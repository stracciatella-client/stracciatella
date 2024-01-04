package net.stracciatella.internal.init.hack.classloader;

import java.io.InputStream;
import java.net.URL;

import net.stracciatella.injected.ClassLoaderAccessor;
import net.stracciatella.internal.module.classloader.StracciatellaClassLoader;

public record ClassLoaderAccessorImpl(StracciatellaClassLoader loader) implements ClassLoaderAccessor {
    @Override
    public URL accessorGetResource(String name) {
        return loader.accessorGetResource(name);
    }

    @Override
    public URL accessorFindResource(String name) {
        return loader.accessorFindResource(name);
    }

    @Override
    public InputStream accessorGetResourceAsStream(String name) {
        return loader.accessorGetResourceAsStream(name);
    }
}

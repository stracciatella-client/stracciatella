package net.stracciatella.init.hack.classloader;

import java.io.InputStream;
import java.net.URL;

import net.stracciatella.init.transform.TransformerRegistry;
import net.stracciatella.injected.ClassLoaderAccessor;
import net.stracciatella.module.classloader.StracciatellaClassLoader;

public record ClassLoaderAccessorImpl(StracciatellaClassLoader loader, TransformerRegistry transformerRegistry) implements ClassLoaderAccessor {
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

    @Override
    public byte[] transform(String className, byte[] bytes) {
        return transformerRegistry.transform(className, bytes);
    }
}

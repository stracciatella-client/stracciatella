package net.stracciatella.init.hack.classloader;

import java.io.InputStream;
import java.net.URL;

import net.stracciatella.module.classloader.StracciatellaClassLoader;
import org.jetbrains.annotations.Nullable;

public class FabricParentClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final ClassLoader parent;
    private final StracciatellaClassLoader stracciatellaClassLoader;

    public FabricParentClassLoader(ClassLoader parent, StracciatellaClassLoader stracciatellaClassLoader) {
        super(parent);
        this.parent = parent;
        this.stracciatellaClassLoader = stracciatellaClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // System.out.println("FABRIC load class " + name);
        synchronized (getClassLoadingLock(name)) {
            var cls = findLoadedClass(name);
            if (cls == null) {
                try {
                    cls = parent.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    // ignore this
                }
                if (cls == null) {
                    cls = stracciatellaClassLoader.loadInAll(name, resolve);
                    if (cls != null) return cls;
                }
            }
            if (cls == null) throw new ClassNotFoundException(name);
            if (resolve) resolveClass(cls);
            return cls;
        }
    }

    @Nullable
    @Override
    public URL getResource(String name) {
        var url = parent.getResource(name);
        if (url != null) return url;
        url = stracciatellaClassLoader.getResourceInAll(name);
        if (url != null) return url;
        return findResource(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("Find class " + name);
        return super.findClass(name);
    }

    @Nullable
    @Override
    public InputStream getResourceAsStream(String name) {
        // System.out.println("FABRIC get as stream");
        return super.getResourceAsStream(name);
    }

    @Override
    protected URL findResource(String name) {
        // System.out.println("FABRIC Find resource " + name);
        return super.findResource(name);
    }
}

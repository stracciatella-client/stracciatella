package net.stracciatella.module.classloader;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleModuleClassLoader extends ClassLoader implements ModuleClassLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModuleClassLoader");

    static {
        registerAsParallelCapable();
    }

    private final Collection<ModuleClassLoader> dependencyLoaders = new CopyOnWriteArraySet<>();
    private final Collection<FileSystem> fileSystems = new CopyOnWriteArraySet<>();
    private final Queue<Reference<Class<?>>> loadedClasses = new ConcurrentLinkedQueue<>();
    private final StracciatellaClassLoader parent;

    public SimpleModuleClassLoader(StracciatellaClassLoader parent) {
        super(parent);
        this.parent = parent;
    }

    public Collection<FileSystem> fileSystems() {
        return fileSystems;
    }

    @Override
    protected URL findResource(String name) {
        for (var fs : fileSystems) {
            var path = fs.getPath(name);
            if (Files.exists(path)) {
                try {
                    return path.toUri().toURL();
                } catch (MalformedURLException e) {
                    LOGGER.error("Failed to convert path to URL", e);
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cls;
        try {
            cls = findClassSafe(name);
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to load class " + name + " because an IO Error occurred", e);
        }
        if (cls != null) return cls;
        return super.findClass(name);
    }

    private @Nullable Class<?> findClassSafe(String name) throws IOException {
        var fileName = name.replace('.', '/').concat(".class");

        var url = findResource(fileName);
        if (url != null) return defineClass(name, url);
        return null;
    }

    private Class<?> defineClass(String name, URL url) throws IOException {
        var connection = url.openConnection();
        var in = connection.getInputStream();
        var array = in.readAllBytes();
        in.close();
        var cls = defineClass(name, array, 0, array.length, null);
        loadedClasses.offer(new WeakReference<>(cls));
        return cls;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var cls = loadClass(name, resolve, true);
        if (cls == null) throw new ClassNotFoundException(name + " not found");
        return cls;
    }

    private @Nullable Class<?> loadClass(String name, boolean resolve, boolean parent) {
        synchronized (getClassLoadingLock(name)) {
            var cls = findLoadedClass(name);
            if (cls == null) {
                if (parent) {
                    // We do not resolve in the parent because we resolve here
                    cls = this.parent.loadClassFromChild(name, resolve);
                    if (cls != null) return cls;
                }
                for (var dependency : dependencyLoaders) {
                    cls = dependency.loadClassFromParent(name);
                    if (cls != null) break;
                }
                if (cls == null) {
                    try {
                        cls = findClassSafe(name);
                    } catch (IOException e) {
                        LOGGER.error("Failed to load class " + name + " because of an IO Error", e);
                    }
                }
            }
            if (resolve && cls != null) resolveClass(cls);
            return cls;
        }
    }

    public Queue<Reference<Class<?>>> loadedClasses() {
        return loadedClasses;
    }

    @Override
    public Collection<ModuleClassLoader> dependencyLoaders() {
        return dependencyLoaders;
    }

    @Override
    public @Nullable Class<?> loadClassFromParent(String name) {
        return loadClass(name, false, false);
    }

    @Override
    public @Nullable URL getResourceFromParent(String name) {
        for (var dependencyLoader : dependencyLoaders) {
            var url = dependencyLoader.getResourceFromParent(name);
            if (url != null) return url;
        }
        return findResource(name);
    }
}

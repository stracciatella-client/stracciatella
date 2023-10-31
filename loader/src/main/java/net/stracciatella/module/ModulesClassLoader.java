package net.stracciatella.module;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

public class ModulesClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final Collection<ModuleClassLoader> moduleLoaders = new CopyOnWriteArrayList<>();

    public ModulesClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    public Collection<ModuleClassLoader> moduleLoaders() {
        return moduleLoaders;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var cls = loadClassFromChild(name, resolve);
        if (cls != null) return cls;
        throw new ClassNotFoundException(name + " not found");
    }

    public @Nullable Class<?> loadClassFromChild(String name, boolean resolve) {
        synchronized (getClassLoadingLock(name)) {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException ignored) {
                // ignore this
            }
            // if (child != null) {
            //     for (var loader : child.dependencyLoaders()) {
            //         var cls = loader.loadClassFromParent(name);
            //         if (cls != null) return cls;
            //     }
            // }
        }
        return null;
    }
}

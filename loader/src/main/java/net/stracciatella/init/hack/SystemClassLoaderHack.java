package net.stracciatella.init.hack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.stracciatella.Stracciatella;
import net.stracciatella.internal.unsafe.UnsafeHelper;
import net.stracciatella.module.StracciatellaThrowables;

@SuppressWarnings({"unchecked", "deprecation"})
public class SystemClassLoaderHack {
    private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    private static final ConcurrentHashMap<String, Certificate[]> systemClassLoaderPackage2certs;
    private static final Path systemClassLoaderInjectionsDirectory;
    private static final Set<Path> knotValidParentCodeSources;
    private static final long builtinClassLoaderUcpOffset;
    private static final WrapperUrlClassLoader wrapper;
    private static final Class<?> reflectionCls;
    private static final long reflectionFieldFilterMapOffset;

    static {
        try {
            systemClassLoaderInjectionsDirectory = Stracciatella.instance().service(Path.class, Stracciatella.WORKING_DIRECTORY).resolve("system_injections");
            // carefully hack into java reflection itself to gain access to hidden fields and unsafe modifications
            var unsafe = UnsafeHelper.unsafe();
            builtinClassLoaderUcpOffset = unsafe.objectFieldOffset(Class.forName("jdk.internal.loader.BuiltinClassLoader").getDeclaredField("ucp"));
            var knotLoader = Stracciatella.class.getClassLoader();
            var delegateField = knotLoader.getClass().getDeclaredField("delegate");
            delegateField.setAccessible(true);
            var delegate = delegateField.get(knotLoader);
            var validParentCodeSourcesField = delegate.getClass().getDeclaredField("validParentCodeSources");
            validParentCodeSourcesField.setAccessible(true);
            knotValidParentCodeSources = (Set<Path>) validParentCodeSourcesField.get(delegate);

            wrapper = new WrapperUrlClassLoader();
            var wrapperUcpOffset = unsafe.objectFieldOffset(URLClassLoader.class.getDeclaredField("ucp"));
            var ucp = unsafe.getObject(systemClassLoader, builtinClassLoaderUcpOffset);
            unsafe.putObject(wrapper, wrapperUcpOffset, ucp);
            reflectionCls = Class.forName("jdk.internal.reflect.Reflection");
            reflectionFieldFilterMapOffset = unsafe.staticFieldOffset(StaticFieldTest.class.getDeclaredField("m1"));
            var handle = disableReflectionFieldFilters();
            var package2certsOffset = unsafe.objectFieldOffset(ClassLoader.class.getDeclaredField("package2certs"));
            systemClassLoaderPackage2certs = (ConcurrentHashMap<String, Certificate[]>) unsafe.getObject(systemClassLoader, package2certsOffset);
            restoreReflectionFieldFilters(handle);
        } catch (Throwable t) {
            throw StracciatellaThrowables.propagate(t);
        }
    }

    public static Path systemClassLoaderInjectionsDirectory() {
        return systemClassLoaderInjectionsDirectory;
    }

    public static Class<?> safeInitializeClass(String name) throws ClassNotFoundException {
        var i = name.lastIndexOf('.');
        var packageName = i == -1 ? "" : name.substring(0, i);
        var old = systemClassLoaderPackage2certs.remove(packageName);
        var cls = Class.forName(name, true, systemClassLoader);
        if (old != null) systemClassLoaderPackage2certs.put(packageName, old);
        return cls;
    }

    public static void insertIntoSystemClassLoaderUnsafe(URL url) {
        wrapper.addURL(url);
        try {
            var path = Path.of(url.toURI()).toRealPath();
            knotValidParentCodeSources.add(path);
        } catch (IOException | URISyntaxException e) {
            throw StracciatellaThrowables.propagate(e);
        }
    }

    private static Object disableReflectionFieldFilters() {
        var unsafe = UnsafeHelper.unsafe();
        var old = unsafe.getObjectVolatile(reflectionCls, reflectionFieldFilterMapOffset);
        unsafe.putObjectVolatile(reflectionCls, reflectionFieldFilterMapOffset, Map.of());
        return old;
    }

    private static void restoreReflectionFieldFilters(Object handle) {
        var unsafe = UnsafeHelper.unsafe();
        unsafe.putObjectVolatile(reflectionCls, reflectionFieldFilterMapOffset, handle);
    }
}

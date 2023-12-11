package net.stracciatella.init.hack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import net.stracciatella.Stracciatella;
import net.stracciatella.init.hack.classloader.ClassDelegateTransformer;
import net.stracciatella.init.hack.classloader.ClassLoaderTransformer;
import net.stracciatella.init.hack.classloader.FabricParentClassLoader;
import net.stracciatella.init.hack.classloader.Generator;
import net.stracciatella.init.hack.classloader.Transformer;
import net.stracciatella.internal.unsafe.UnsafeHelper;
import net.stracciatella.module.classloader.StracciatellaClassLoader;
import org.objectweb.asm.commons.SimpleRemapper;

public class KnotClassLoaderHack {
    public static final String INJECTED_PATH = "injected.jar";
    public static final String KNOT_LOADER = "net.fabricmc.loader.impl.launch.knot.KnotClassLoader";
    public static final String CUSTOM_LOADER = "net.fabricmc.loader.impl.launch.knot.StracciatellaCustomKnotClassLoader";
    public static final String KNOT_DELEGATE = "net.fabricmc.loader.impl.launch.knot.KnotClassDelegate";
    public static final String CUSTOM_DELEGATE = "net.fabricmc.loader.impl.launch.knot.StracciatellaCustomKnotClassDelegate";
    private static final Map<String, String> mapping;
    private static final Map<String, String> remapperMapping;

    static {
        mapping = new HashMap<>();
        mapping(KNOT_LOADER, CUSTOM_LOADER);
        mapping(KNOT_DELEGATE, CUSTOM_DELEGATE);
        remapperMapping = new HashMap<>();
        for (var entry : mapping.entrySet()) {
            remapperMapping.put(entry.getKey().replace('.', '/'), entry.getValue().replace('.', '/'));
        }
    }

    public static void hack() throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        var directory = SystemClassLoaderHack.systemClassLoaderInjectionsDirectory();
        Files.createDirectories(directory);

        var injectedPath = directory.resolve(INJECTED_PATH);
        try (var in = KnotClassLoaderHack.class.getClassLoader().getResourceAsStream(INJECTED_PATH)) {
            Files.copy(Objects.requireNonNull(in, INJECTED_PATH + " not found in jar"), injectedPath, StandardCopyOption.REPLACE_EXISTING);
        }
        SystemClassLoaderHack.insertIntoSystemClassLoaderUnsafe(injectedPath.toUri().toURL());

        var path = directory.resolve("knot_hack.jar");
        var out = new JarOutputStream(Files.newOutputStream(path));
        addClassCustom(out, KNOT_LOADER, new ClassLoaderTransformer());
        addClassCustom(out, KNOT_DELEGATE, new ClassDelegateTransformer());
        out.close();
        SystemClassLoaderHack.insertIntoSystemClassLoaderUnsafe(path.toUri().toURL());

        var classDelegateCls = SystemClassLoaderHack.safeInitializeClass(CUSTOM_DELEGATE);
        var classLoaderCls = SystemClassLoaderHack.safeInitializeClass(CUSTOM_LOADER);
        var classLoader = KnotClassLoaderHack.class.getClassLoader();

        var f = Class.forName(KNOT_LOADER).getDeclaredField("delegate");
        f.setAccessible(true);
        var delegate = f.get(classLoader);
        delegate = UnsafeHelper.unsafeCast(delegate, classDelegateCls);
        UnsafeHelper.unsafeCast(classLoader, classLoaderCls.asSubclass(ClassLoader.class));
        f = delegate.getClass().getDeclaredField("parentClassLoader");
        f.setAccessible(true);
        var stracciatellaLoader = Stracciatella.instance().service(StracciatellaClassLoader.class);
        f.set(delegate, new FabricParentClassLoader((ClassLoader) f.get(delegate), stracciatellaLoader));
    }

    private static void addClass(JarOutputStream out, String className, byte[] data) throws IOException {
        out.putNextEntry(new ZipEntry(className.replace('.', '/') + ".class"));
        out.write(data);
        out.closeEntry();
    }

    private static void addClassGenerated(JarOutputStream out, Generator generator) throws IOException {
        var node = generator.generate();
        var name = node.name.replace('/', '.');
        var data = ClassCopyHack.toByteArray(node);
        addClass(out, name, data);
    }

    private static void addClassCustom(JarOutputStream out, String className, Transformer transformer) throws IOException {
        var data = custom(transformer, className);
        addClass(out, mapping.get(className), data);
    }

    private static byte[] custom(Transformer transformer, String className) throws IOException {
        var remapper = new SimpleRemapper(remapperMapping);
        var node = ClassCopyHack.createCopyRemapped(className, remapper);
        if (transformer != null) transformer.transform(node);
        return ClassCopyHack.toByteArray(node);
    }

    private static void mapping(String oldName, String newName) {
        mapping.put(oldName, newName);
    }
}

package net.stracciatella.internal.module.classloader;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class StracciatellaClassLoader extends SecureClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final Collection<ModuleClassLoader> moduleLoaders = new CopyOnWriteArrayList<>();

    public StracciatellaClassLoader() {
        super(StracciatellaClassLoader.class.getClassLoader());
    }

    public Collection<ModuleClassLoader> moduleLoaders() {
        return moduleLoaders;
    }

    @Nullable
    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }

    public URL getResourceInAll(String name) {
        for (var moduleLoader : moduleLoaders) {
            var url = moduleLoader.getResourceFromParent(name);
            if (url != null) return url;
        }
        return findResource(name);
    }

    @Override
    protected URL findResource(String name) {
        return super.findResource(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals("net.stracciatella.FindMe")) {
            System.out.println("Generate findme");
            var node = new ClassNode();
            node.visit(V21, ACC_PUBLIC, name.replace('.', '/'), null, getType(Object.class).getInternalName(), new String[0]);
            node.visitSource("<dynamic>", null);
            var mv = node.visitMethod(ACC_STATIC | ACC_PUBLIC, "test", "()V", null, null);
            mv.visitFieldInsn(GETSTATIC, getInternalName(System.class), "out", getDescriptor(PrintStream.class));
            mv.visitLdcInsn("Execute FindMe test");
            mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(PrintStream.class), "println", getMethodDescriptor(VOID_TYPE, getType(String.class)), false);

            mv.visitFieldInsn(GETSTATIC, getInternalName(System.class), "out", getDescriptor(PrintStream.class));
            mv.visitLdcInsn(getObjectType("net/stracciatella/FindMe"));
            mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(Class.class), "getClassLoader", getMethodDescriptor(getType(ClassLoader.class)), false);
            mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(PrintStream.class), "println", getMethodDescriptor(VOID_TYPE, getType(Object.class)), false);
            mv.visitInsn(RETURN);
            var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(cw);
            var data = cw.toByteArray();
            return defineClass(name, data, 0, data.length);
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var cls = loadClassFromChild(name, resolve);
        if (cls != null) return cls;
        throw new ClassNotFoundException(name + " not found");
    }

    public @Nullable Class<?> loadInAll(String name, boolean resolve) {
        synchronized (getClassLoadingLock(name)) {
            var cls = findLoadedClass(name);
            if (cls != null) {
                if (resolve) resolveClass(cls);
                return cls;
            }
            for (var moduleLoader : moduleLoaders) {
                cls = moduleLoader.loadClassFromParent(name);
                if (cls == null) continue;
                if (resolve) resolveClass(cls);
                return cls;
            }
            try {
                cls = findClass(name);
            } catch (ClassNotFoundException ignored) {
                // ignore this
            }
            if (resolve && cls != null) resolveClass(cls);

            return cls;
        }
    }

    public @Nullable Class<?> loadClassFromChild(String name, boolean resolve) {
        // synchronized (getClassLoadingLock(name)) {
        if (name.startsWith("net.minecraft")) System.out.println("load " + name);
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
        // }
        return null;
    }

    public InputStream accessorGetResourceAsStream(String name) {
        var url = accessorGetResource(name);
        if (url == null) return null;
        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    public URL accessorFindResource(String name) {
        for (var moduleLoader : moduleLoaders) {
            var url = moduleLoader.getResourceFromParent(name);
            if (url != null) return url;
        }
        return null;
    }

    public URL accessorGetResource(String name) {
        for (var moduleLoader : moduleLoaders) {
            var url = moduleLoader.getResourceFromParent(name);
            if (url != null) return url;
        }
        return null;
    }
}

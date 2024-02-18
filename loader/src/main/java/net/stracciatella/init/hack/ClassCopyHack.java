package net.stracciatella.init.hack;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

public class ClassCopyHack {

    public static ClassReader createCopy(String className) throws IOException {
        try (var in = ClassCopyHack.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) throw new NoClassDefFoundError("Class " + className + " not found!");
            var data = in.readAllBytes();
            return new ClassReader(data);
        }
    }

    public static byte[] toByteArray(ClassNode node) {
        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(cw);
        return cw.toByteArray();
    }

    public static ClassNode createCopyRemapped(String className, Remapper remapper) throws IOException {
        var r = createCopy(className);
        var cn = new ClassNode();
        r.accept(new ClassRemapper(cn, remapper), 0);
        return cn;
    }

    public static ClassNode createCopyRemapped(String className, String remappedName) throws IOException {
        var remapper = new SimpleRemapper(className.replace('.', '/'), remappedName.replace('.', '/'));
        return createCopyRemapped(className, remapper);
    }

}

package net.stracciatella.init.hack.classloader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.stracciatella.init.transform.ClassByteTransformer;
import net.stracciatella.init.transform.ClassNodeTransformer;
import net.stracciatella.init.transform.TransformerRegistry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class DefaultTransformerRegistry implements TransformerRegistry {
    private final List<ClassByteTransformer> globalClassByteTransformers = new CopyOnWriteArrayList<>();
    private final List<ClassNodeTransformer> globalClassNodeTransformers = new CopyOnWriteArrayList<>();
    private final Map<String, List<ClassByteTransformer>> classByteTransformers = new ConcurrentHashMap<>();
    private final Map<String, List<ClassNodeTransformer>> classNodeTransformers = new ConcurrentHashMap<>();

    @Override
    public byte[] transform(String className, byte[] classBytes) {
        if (!globalClassByteTransformers.isEmpty()) {
            for (var transformer : globalClassByteTransformers) {
                classBytes = transformer.transform(className, classBytes);
            }
        }
        if (classByteTransformers.containsKey(className)) {
            for (var classByteTransformer : classByteTransformers.get(className)) {
                classBytes = classByteTransformer.transform(className, classBytes);
            }
        }

        ClassNode node = null;
        ClassReader reader = null;
        if (!globalClassNodeTransformers.isEmpty()) {
            reader = reader(null, classBytes);
            node = node(null, reader);
            for (var transformer : globalClassNodeTransformers) {
                transformer.transform(className, node);
            }
        }
        if (classNodeTransformers.containsKey(className)) {
            reader = reader(reader, classBytes);
            node = node(node, reader);
            for (var classNodeTransformer : classNodeTransformers.get(className)) {
                classNodeTransformer.transform(className, node);
            }
        }
        if (node != null) {
            var writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        }
        return classBytes;
    }

    private ClassReader reader(ClassReader reader, byte[] classBytes) {
        if (reader != null) return reader;
        return new ClassReader(classBytes);
    }

    private ClassNode node(ClassNode node, ClassReader reader) {
        if (node != null) return node;
        node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    @Override
    public void registerTransformer(ClassByteTransformer transformer) {
        globalClassByteTransformers.add(transformer);
    }

    @Override
    public void registerTransformer(ClassNodeTransformer transformer) {
        globalClassNodeTransformers.add(transformer);
    }

    @Override
    public void registerTransformer(String className, ClassByteTransformer transformer) {
        classByteTransformers.computeIfAbsent(className, unused -> new CopyOnWriteArrayList<>()).add(transformer);
    }

    @Override
    public void registerTransformer(String className, ClassNodeTransformer transformer) {
        classNodeTransformers.computeIfAbsent(className, unused -> new CopyOnWriteArrayList<>()).add(transformer);
    }
}

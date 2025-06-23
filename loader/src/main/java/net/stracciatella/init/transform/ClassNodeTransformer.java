package net.stracciatella.init.transform;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeTransformer {
    void transform(String name, ClassNode node);
}

package net.stracciatella.init.hack.classloader;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import net.stracciatella.injected.StracciatellaInjections;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassDelegateTransformer implements Transformer {
    @Override
    public void transform(ClassNode node) {
        for (var method : node.methods) {
            if (method.name.equals("initializeTransformers")) {
                initializeTransformers(method);
            }
        }
    }

    private void initializeTransformers(MethodNode method) {
        for (var c : method.instructions) {
            if (c.getOpcode() != RETURN) continue;
            var instructions = new InsnList();
            instructions.add(new MethodInsnNode(INVOKESTATIC, getInternalName(StracciatellaInjections.Holder.class), "initializeMixins", getMethodDescriptor(VOID_TYPE)));
            method.instructions.insertBefore(c, instructions);
            break;
        }
    }
}

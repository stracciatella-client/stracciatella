package net.stracciatella.init.hack.classloader;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import net.stracciatella.injected.ClassLoaderWrapper;
import net.stracciatella.injected.StracciatellaInjections;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassDelegateTransformer implements Transformer {
    @Override
    public void transform(ClassNode node) {
        for (var method : node.methods) {
            if (method.name.equals("initializeTransformers")) {
                initializeTransformers(method);
            } else if (method.name.equals("getPostMixinClassByteArray")) {
                loadClass(method);
            }
        }
    }

    private void loadClass(MethodNode method) {
        for (var n : method.instructions) {
            if (n instanceof MethodInsnNode mn && mn.name.equals("getPreMixinClassByteArray")) {
                var ins = new InsnList();
                ins.add(new VarInsnNode(ALOAD, 1));
                ins.add(new MethodInsnNode(INVOKESTATIC, getInternalName(ClassLoaderWrapper.class), "transform", getMethodDescriptor(getType(byte[].class), getType(byte[].class), getType(String.class))));
                method.instructions.insert(n, ins);
                break;
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

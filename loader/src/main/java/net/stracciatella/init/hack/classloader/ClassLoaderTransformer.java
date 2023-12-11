package net.stracciatella.init.hack.classloader;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import java.io.InputStream;
import java.net.URL;

import net.stracciatella.injected.ClassLoaderWrapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassLoaderTransformer implements Transformer {
    @Override
    public void transform(ClassNode node) {
        for (var method : node.methods) {
            switch (method.name) {
                case "getResourceAsStream" -> getResourceAsStream(method);
                case "getResource" -> getResource(method);
                case "findResource" -> findResource(method);
                case "findResourceFwd" -> findResourceFwd(node, method);
                default -> {
                }
            }
        }
    }

    private void findResource(MethodNode method) {
        for (var c : method.instructions) {
            if (c.getOpcode() != ARETURN) continue;
            var start = new LabelNode();
            var end = new LabelNode();
            method.localVariables.add(new LocalVariableNode("url", getDescriptor(URL.class), null, start, end, 2));
            var instructions = new InsnList();
            instructions.add(start);
            instructions.add(new InsnNode(DUP));
            instructions.add(new VarInsnNode(ASTORE, 2));
            // if "url" not null, then jump to end
            var preEnd = new LabelNode();
            instructions.add(new JumpInsnNode(IFNONNULL, preEnd));

            instructions.add(new VarInsnNode(ALOAD, 1));
            instructions.add(new MethodInsnNode(INVOKESTATIC, getInternalName(ClassLoaderWrapper.class), "findResource", getMethodDescriptor(getType(URL.class), getType(String.class))));
            instructions.add(new VarInsnNode(ASTORE, 2));

            instructions.add(preEnd);
            // load "url" into stack to prepare for return
            instructions.add(new VarInsnNode(ALOAD, 2));
            instructions.add(end);
            method.instructions.insertBefore(c, instructions);
            break;
        }
    }

    private void findResourceFwd(ClassNode node, MethodNode method) {
        var instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ALOAD, 1));
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, "findResource", getMethodDescriptor(getType(URL.class), getType(String.class)), false));
        instructions.add(new InsnNode(ARETURN));
        method.instructions = instructions;
    }

    private void getResourceAsStream(MethodNode method) {
        var it = method.instructions.iterator();
        while (it.hasNext()) {
            var c = it.next();
            if (c.getOpcode() != ARETURN) continue;
            c = it.previous();
            c = it.previous();
            var instructions = new InsnList();

            // if(name != null) GOTO label
            instructions.add(new VarInsnNode(ALOAD, 2));
            var label = new LabelNode();
            instructions.add(new JumpInsnNode(IFNONNULL, label));

            // Load "name" for method call
            instructions.add(new VarInsnNode(ALOAD, 1));
            instructions.add(new MethodInsnNode(INVOKESTATIC, getInternalName(ClassLoaderWrapper.class), "getResourceAsStream", getMethodDescriptor(getType(InputStream.class), getType(String.class))));
            instructions.add(new VarInsnNode(ASTORE, 2));

            instructions.add(label);

            method.instructions.insertBefore(c, instructions);
            break;
        }
    }

    private void getResource(MethodNode method) {
        var it = method.instructions.iterator();
        while (it.hasNext()) {
            var c = it.next();
            if (c.getOpcode() != ARETURN) continue;
            c = it.previous();
            var instructions = new InsnList();

            // if(url != null) GOTO label
            instructions.add(new VarInsnNode(ALOAD, 2));
            var label = new LabelNode();
            instructions.add(new JumpInsnNode(IFNONNULL, label));

            instructions.add(new VarInsnNode(ALOAD, 1));
            instructions.add(new MethodInsnNode(INVOKESTATIC, getInternalName(ClassLoaderWrapper.class), "getResource", getMethodDescriptor(getType(URL.class), getType(String.class))));
            instructions.add(new VarInsnNode(ASTORE, 2));

            instructions.add(label);

            method.instructions.insertBefore(c, instructions);
            break;
        }
    }
}

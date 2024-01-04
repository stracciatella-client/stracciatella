package net.stracciatella.internal.init.hack.classloader;

import org.objectweb.asm.tree.ClassNode;

public interface Generator {
    ClassNode generate();
}

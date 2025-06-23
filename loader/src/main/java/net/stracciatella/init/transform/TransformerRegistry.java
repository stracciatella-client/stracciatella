package net.stracciatella.init.transform;

import net.stracciatella.Stracciatella;

public interface TransformerRegistry {
    byte[] transform(String className, byte[] classBytes);

    void registerTransformer(ClassByteTransformer transformer);

    void registerTransformer(ClassNodeTransformer transformer);

    void registerTransformer(String className, ClassByteTransformer transformer);

    void registerTransformer(String className, ClassNodeTransformer transformer);

    static TransformerRegistry instance() {
        return Stracciatella.instance().service(TransformerRegistry.class);
    }
}

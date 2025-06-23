package net.stracciatella.init.transform;

public interface ClassByteTransformer {
    byte[] transform(String className, byte[] classBytes);
}

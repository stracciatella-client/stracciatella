package net.stracciatella.internal.util;

import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;

public class NullOutputStream extends OutputStream {
    public static final NullOutputStream INSTANCE = new NullOutputStream();

    private NullOutputStream() {
    }

    @Override
    public void write(int b) {
    }

    @Override
    public void write(byte @NotNull [] b) {
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}

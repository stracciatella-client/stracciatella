package net.stracciatella.internal.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import net.stracciatella.util.Provider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class LazyProvider<T> implements Provider<T> {
    private final AtomicReference<T> object = new AtomicReference<>();
    private volatile Supplier<T> supplier;

    public LazyProvider(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        var current = object.getPlain();
        if (current != null) return current;
        synchronized (object) {
            current = object.get();
            if (current != null) return current;
            current = supplier.get();
            supplier = null;
            object.set(current);
        }
        return current;
    }
}

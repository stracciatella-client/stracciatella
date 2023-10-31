package net.stracciatella.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface Provider<T> {
    static <T> Provider<T> of(Supplier<T> supplier) {
        return new Provider<>() {
            private final AtomicReference<T> object = new AtomicReference<>();

            @Override
            public T get() {
                var current = object.getPlain();
                if (current != null) return current;
                synchronized (object) {
                    current = object.get();
                    if (current != null) return current;
                    current = supplier.get();
                    object.set(current);
                }
                return current;
            }
        };
    }

    T get();
}

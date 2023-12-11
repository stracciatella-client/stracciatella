package net.stracciatella.util;

import java.util.function.Supplier;

import net.stracciatella.internal.util.LazyProvider;

public interface Provider<T> {
    /**
     * Creates a Provider for a Supplier. The supplier is executed once, when the Provider first needs the value.<br>
     * After that the Supplier is not executed again.<br>
     * The Provider is thread-safe.<br>
     * The supplier will be eligible for garbage-collection after the value has bene set.
     *
     * @param supplier a supplier for the value
     * @param <T>      the type
     * @return a threadsafe provider for the supplier
     */
    static <T> Provider<T> of(Supplier<T> supplier) {
        return new LazyProvider<>(supplier);
    }

    T get();
}

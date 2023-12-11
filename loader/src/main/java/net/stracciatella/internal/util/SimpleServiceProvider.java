package net.stracciatella.internal.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.stracciatella.util.Provider;
import net.stracciatella.util.ServiceProvider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SimpleServiceProvider implements ServiceProvider {

    private final ConcurrentHashMap<String, Provider<?>> services = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, List<Provider<?>>> servicesByType = new ConcurrentHashMap<>();

    public <T> T service(Class<T> type) {
        var services = servicesByType.get(type);
        if (services == null) return null;
        var it = services.iterator();
        if (!it.hasNext()) return null;
        var provider = it.next();
        if (provider == null) return null;
        return type.cast(provider.get());
    }

    @Override
    public <T> T service(Class<T> type, String name) {
        var provider = services.get(name);
        if (provider == null) return null;
        return type.cast(provider.get());
    }

    @Override
    public <T> void registerProvider(String name, Class<T> type, Provider<? extends T> serviceProvider) {
        if (services.putIfAbsent(name, serviceProvider) != null) throw new IllegalStateException("Service already registered: " + name);
        servicesByType.compute(type, (t, s) -> {
            if (s == null) s = new CopyOnWriteArrayList<>();
            s.add(serviceProvider);
            return s;
        });
    }

    @SuppressWarnings("unchecked")
    public <T> Provider<T> unregister(String name, Class<T> type) {
        var provider = services.remove(name);
        if (provider == null) throw new IllegalStateException("Service not registered: " + name);
        servicesByType.compute(type, (t, s) -> {
            if (s == null) throw new IllegalStateException("No services registered for type");
            if (!s.remove(provider)) throw new IllegalStateException("Service was not registered for type");
            if (s.isEmpty()) return null;
            return s;
        });
        return (Provider<T>) provider;
    }
}

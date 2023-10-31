package net.stracciatella.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ServiceProvider {

    <T> T service(Class<T> type);

    <T> T service(Class<T> type, String name);

    default <T> void register(String name, Class<T> type, T service) {
        registerProvider(name, type, () -> service);
    }

    <T> void registerProvider(String name, Class<T> type, Provider<? extends T> serviceProvider);

    <T> Provider<T> unregister(String name, Class<T> type);

    interface Wrapper extends ServiceProvider {
        ServiceProvider serviceProvider();

        @Override
        default <T> T service(Class<T> type) {
            return serviceProvider().service(type);
        }

        @Override
        default <T> T service(Class<T> type, String name) {
            return serviceProvider().service(type, name);
        }

        default <T> void registerProvider(String name, Class<T> type, Provider<? extends T> serviceProvider) {
            serviceProvider().registerProvider(name, type, serviceProvider);
        }

        @Override
        default <T> Provider<T> unregister(String name, Class<T> type) {
            return serviceProvider().unregister(name, type);
        }
    }

    class SimpleServiceProvider implements ServiceProvider {

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
}
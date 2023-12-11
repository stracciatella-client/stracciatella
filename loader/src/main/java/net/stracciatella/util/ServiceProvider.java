package net.stracciatella.util;

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
}
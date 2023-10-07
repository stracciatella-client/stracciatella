package net.stracciatella.config;

/**
 * This is a wrapper class used to store cofig values.
 * * @param T needs to be serializable by the Gson lib.
 */
public class ConfigurationAttribute<T> {

    T defaultValue;
    String key;
    T loadedValue = null;

    public ConfigurationAttribute(T defaultValue, String key) {
        this.defaultValue = defaultValue;
        this.key = key;
    }

    public T getValue() {
        if (loadedValue == null) {
            return defaultValue;
        }
        return loadedValue;
    }
}

package net.stracciatella.config;

/**
 * This is a wrapper class used to store config values.
 * * @param T needs to be serializable by the Gson lib.
 */
public record ConfigurationAttribute<T>(String key, Class<T> type, T defaultValue) {

}

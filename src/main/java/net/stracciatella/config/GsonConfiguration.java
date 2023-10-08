package net.stracciatella.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

class GsonConfiguration implements Configuration {
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Gson gson;
    // We do not know the initial type of the json objects, so we need to store the serialized form until we do know it.
    // This will be cleared as values are loaded as not to waste memory
    private final Map<String, JsonElement> inputValues = new ConcurrentHashMap<>();
    private final @Nullable Path path;

    public GsonConfiguration(Gson gson, Map<String, JsonElement> inputValues, @Nullable Path path) {
        this.gson = gson;
        this.path = path;
        if (inputValues != null) this.inputValues.putAll(inputValues);
    }

    @SuppressWarnings("unchecked")
    public <T> T value(ConfigurationAttribute<T> attribute) {
        AtomicBoolean save = new AtomicBoolean(false);
        var value = (T) attributes.computeIfAbsent(attribute.key(), key -> {
            save.setPlain(true);
            if (inputValues.containsKey(key)) {
                return gson.fromJson(inputValues.remove(key), attribute.type());
            }
            return attribute.defaultValue();
        });
        if (save.getPlain()) save(path);
        return value;
    }

    @Override
    public <T> void value(ConfigurationAttribute<T> attribute, T value) {
        attributes.put(attribute.key(), value);
        inputValues.remove(attribute.key());
        save(path);
    }

    private void save(Path path) {
        if (path == null) return;
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            var json = new JsonObject();
            if (!inputValues.isEmpty()) {
                for (var entry : inputValues.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }
            for (var entry : attributes.entrySet()) {
                json.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
            }
            gson.toJson(json, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

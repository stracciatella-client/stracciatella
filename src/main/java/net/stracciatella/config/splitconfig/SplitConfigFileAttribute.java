package net.stracciatella.config.splitconfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitConfigFileAttribute<T> {
    private static final Logger logger = LoggerFactory.getLogger(SplitConfigFileAttribute.class);
    private static final Map<String, SplitConfigFileAttribute<?>> allAttributes = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private final Path path;
    private final T defaultValue;
    private T value;
    private final Type type;

    public SplitConfigFileAttribute(T defaultValue, String path, Type typeParameterClass) {
        this.path = Path.of("config", path);
        this.defaultValue = defaultValue;
        type = typeParameterClass;
        if (allAttributes.putIfAbsent(path, this) != null) {
            throw new IllegalStateException("Duplicate Path is not allowed!");
        }
        value = read();
    }

    public void reload() {
        value = read();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        write(value, 1);
    }

    private T read() {
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                reader.readLine();
                return gson.fromJson(reader, type);
            } catch (IOException e) {
                logger.error("There was an error while reading File: " + path.toAbsolutePath() + ". ", e);
                return defaultValue;
            }
        } else {
            write(defaultValue, 1);
            return defaultValue;
        }
    }

    private void write(T value, int dataVersion) {
        try {
            Files.createDirectories(path.getParent());
            var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            writer.write(Integer.toString(dataVersion));
            writer.newLine();
            writer.write(gson.toJson(value));
            this.value = value;
            writer.close();
        } catch (IOException e) {
            logger.error("error writing file: " + path.toAbsolutePath(), e);
        }
    }

}

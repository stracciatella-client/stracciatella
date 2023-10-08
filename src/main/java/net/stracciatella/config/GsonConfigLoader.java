package net.stracciatella.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GsonConfigLoader implements ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonConfigLoader.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public Configuration loadConfig(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var json = gson.fromJson(reader, JsonObject.class);
            return new GsonConfiguration(gson, json.asMap(), path);
        }
    }
    // public Optional<String> loadConfigFileContent(String path) {
    //     File file = new File(path);
    //
    //     if (file.exists()) {
    //         logger.debug("loaded config file");
    //         try {
    //             String fileData = FileUtils.readFileToString(file, "UTF-8");
    //         } catch (IOException e) {
    //             logger.error("something went wrong when reading config file: " + file.getAbsolutePath());
    //             return Optional.empty();
    //         }
    //     } else {
    //         logger.warn("couldn't find config file at: " + file.getAbsolutePath());
    //         return Optional.empty();
    //     }
    //     return Optional.empty();
    // }
    //
    // @Override
    // public void saveConfig(String content, String path) {
    //     File file = new File(path);
    //     try {
    //         FileUtils.write(file, content, "UTF-8");
    //     } catch (IOException e) {
    //         logger.error("couldn't write config content to file");
    //     }
    // }

}

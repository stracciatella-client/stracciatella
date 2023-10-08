package net.stracciatella.config;

import java.io.IOException;
import java.nio.file.Path;

public interface ConfigLoader {

    ConfigLoader GSON = new GsonConfigLoader();

    Configuration loadConfig(Path path) throws IOException;

}

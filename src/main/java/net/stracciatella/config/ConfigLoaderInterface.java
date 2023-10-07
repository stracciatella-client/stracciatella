package net.stracciatella.config;

import java.util.Optional;

public interface ConfigLoaderInterface {
    Optional<String> loadConfigFileContent(String path);

    void saveConfig(String content, String path);
}

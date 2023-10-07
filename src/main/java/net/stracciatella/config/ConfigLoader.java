package net.stracciatella.config;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLoader implements ConfigLoaderInterface {

    Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    public Optional<String> loadConfigFileContent(String path) {
        File file = new File(path);

        if (file.exists()) {
            logger.debug("loaded config file");
            try {
                String fileData = FileUtils.readFileToString(file, "UTF-8");
            } catch (IOException e) {
                logger.error("something went wrong when reading config file: " + file.getAbsolutePath());
                return Optional.empty();
            }
        } else {
            logger.warn("couldn't find config file at: " + file.getAbsolutePath());
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public void saveConfig(String content, String path) {
        File file = new File(path);
        try {
            FileUtils.write(file, content, "UTF-8");
        } catch (IOException e) {
            logger.error("couldn't write config content to file");
        }
    }

}

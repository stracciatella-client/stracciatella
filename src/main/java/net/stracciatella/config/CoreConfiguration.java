package net.stracciatella.config;

import java.util.HashMap;
import java.util.Optional;

import com.google.gson.Gson;

public class CoreConfiguration {
    public static ConfigLoaderInterface configLoaderInterface = new ConfigLoader();
    static HashMap<String, ConfigurationAttribute> attributes = new HashMap<>();
    private static CoreConfiguration instance;
    String path = "config/coreConfig";

    public CoreConfiguration() {
        Optional<String> s = configLoaderInterface.loadConfigFileContent(path);
        if (s.isPresent()) {
            attributes = new Gson().fromJson(s.get(), HashMap.class);
        }
    }



    public static CoreConfiguration getInstance() {
        if (instance == null) {
            instance = new CoreConfiguration();
        }
        return instance;
    }

    public Object getValue(ConfigurationAttribute attribute) {
        if (attributes.containsKey(attribute.key)) {
            return attributes.get(attribute.key);
        } else {
            attributes.put(attribute.key, attribute);
            return attribute.getValue();
        }
    }

    public void saveToFile() {
        configLoaderInterface.saveConfig(new Gson().toJson(attributes), path);
    }





}

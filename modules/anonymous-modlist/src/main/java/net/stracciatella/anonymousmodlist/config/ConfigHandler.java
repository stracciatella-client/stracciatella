package net.stracciatella.anonymousmodlist.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHandler {
    private static final Path configFile = FabricLoader.getInstance().getConfigDir().resolve("anonymous-modlist.json");
    private static final Logger LOGGER = LogManager.getLogger(ConfigHandler.class);

    private ConfigHandler() {
    }

    private static ConfigHandler INSTANCE = null;

    public static ConfigHandler getInstance() {
        if (INSTANCE == null) {
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(configFile.toFile())) {
                INSTANCE = gson.fromJson(reader, ConfigHandler.class);
            } catch (FileNotFoundException ignored) {
                // Do nothing!
            } catch (IOException e) {
                LOGGER.error("Failed to read configuration", e);
            }
            if (INSTANCE == null) {
                INSTANCE = new ConfigHandler();
                INSTANCE.save();
            }

        }
        return INSTANCE;
    }

    private boolean anonymousModlist = true;





    private transient boolean enabledDirty = false;


    public void setAnonymousModlist(boolean en) {
        anonymousModlist = en;
        save();
    }

    public static void set(boolean en) {
        getInstance().setAnonymousModlist(en);
    }

    public static boolean get() {
        return getInstance().isAnonymousModlistEnabled();
    }

    public boolean isAnonymousModlistEnabled() {
        return anonymousModlist;
    }

    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            gson.toJson(this, ConfigHandler.class, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration", e);
        }
    }


}
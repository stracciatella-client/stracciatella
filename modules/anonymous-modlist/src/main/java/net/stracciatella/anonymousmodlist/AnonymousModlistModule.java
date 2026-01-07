package net.stracciatella.anonymousmodlist;

import java.util.Map;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.entrypoint.EntrypointStorage;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.stracciatella.Stracciatella;
import net.stracciatella.anonymousmodlist.config.SodiumCompat;
import net.stracciatella.module.Module;

public class AnonymousModlistModule implements Module {
    @SuppressWarnings("unchecked")
    @Task(lifeCycle = LifeCycle.INITIALIZED)
    public void init() throws Exception {
        var modContainer = (ModContainerImpl) Stracciatella.instance().service(ModContainer.class, Stracciatella.STRACCIATELLA_MOD_CONTAINER);
        var key = "sodium:config_api_user";
        var metadata = new EntrypointMetadata() {
            @Override
            public String getAdapter() {
                return "default";
            }

            @Override
            public String getValue() {
                return SodiumCompat.class.getName();
            }
        };
        Map<String, LanguageAdapter> adapterMap = Stracciatella.instance().service(Map.class, "adapter_map");

        Stracciatella.instance().service(EntrypointStorage.class).add(modContainer, key, metadata, adapterMap);
    }

    @Task(lifeCycle = LifeCycle.MIXINS)
    public void registerMixins() {
    }
}

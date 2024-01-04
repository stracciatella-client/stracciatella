package net.stracciatella.internal.init.hack;

import java.nio.file.Path;
import java.util.Collection;

import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;
import net.stracciatella.Stracciatella;
import net.stracciatella.internal.module.classloader.StracciatellaClassLoader;

public class ProviderHack implements GameProvider {
    private final GameProvider handle;

    public ProviderHack(GameProvider handle) {
        this.handle = handle;
    }

    @Override
    public String getGameId() {
        return handle.getGameId();
    }

    @Override
    public String getGameName() {
        return handle.getGameName();
    }

    @Override
    public String getRawGameVersion() {
        return handle.getRawGameVersion();
    }

    @Override
    public String getNormalizedGameVersion() {
        return handle.getNormalizedGameVersion();
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        return handle.getBuiltinMods();
    }

    @Override
    public String getEntrypoint() {
        return handle.getEntrypoint();
    }

    @Override
    public Path getLaunchDirectory() {
        return handle.getLaunchDirectory();
    }

    @Override
    public boolean isObfuscated() {
        return handle.isObfuscated();
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return handle.requiresUrlClassLoader();
    }

    @Override
    public boolean isEnabled() {
        return handle.isEnabled();
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        return handle.locateGame(launcher, args);
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        handle.initialize(launcher);
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return handle.getEntrypointTransformer();
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        handle.unlockClassPath(launcher);
    }

    @Override
    public void launch(ClassLoader loader) {
        handle.launch(Stracciatella.instance().service(StracciatellaClassLoader.class));
    }

    @Override
    public Arguments getArguments() {
        return handle.getArguments();
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return handle.getLaunchArguments(sanitize);
    }
}

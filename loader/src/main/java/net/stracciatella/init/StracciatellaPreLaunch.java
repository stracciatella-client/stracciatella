package net.stracciatella.init;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.stracciatella.Stracciatella;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;

public class StracciatellaPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        var stracciatella = Stracciatella.instance();
        ((SimpleModuleManager) stracciatella.service(ModuleManager.class)).changeLifeCycle(Module.LifeCycle.PRE_LAUNCH);
    }
}

package net.stracciatella.init;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.service.MixinService;

public class StracciatellaPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("Stracciatella-PreLaunch");

    @Override
    public void onPreLaunch() {
        var service = MixinService.getService();
        LOGGER.info(service.toString());
        System.out.println("Pre-Launch stracciatella");
    }
}

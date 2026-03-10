package net.stracciatella.testing;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.runner.TestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client gametest entrypoint. Creates a flat test world and runs all
 * registered test suites using the Mojang/Fabric gametest infrastructure.
 */
public class GameTestEntrypoint implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("stracciatella-testing");

    @Override
    public void runTest(ClientGameTestContext context) {
        LOGGER.info("Starting Stracciatella client gametest");

        try (var sp = context.worldBuilder()
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setDifficulty(Difficulty.PEACEFUL);
                    state.setAllowCommands(true);
                })
                .create()) {

            sp.getClientWorld().waitForChunksDownload();

            TestContext ctx = new TestContext(context);
            TestRunner.instance().runAll(ctx);
        }

        LOGGER.info("Stracciatella client gametest finished");
    }
}

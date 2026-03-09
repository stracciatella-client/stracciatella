package net.stracciatella.testing.world;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatically creates or joins the test world when autorun is enabled.
 * Creates a flat creative world with cheats enabled for test execution.
 */
public class AutoTestWorld {
    private static final Logger LOGGER = LoggerFactory.getLogger("stracciatella-testing");
    private static final String WORLD_NAME = "stracciatella-test";
    private static boolean triggered = false;

    public static void joinOrCreate() {
        if (triggered) return;
        triggered = true;

        Minecraft mc = Minecraft.getInstance();

        if (mc.getLevelSource().levelExists(WORLD_NAME)) {
            LOGGER.info("[Testing] Opening existing test world '{}'", WORLD_NAME);
            mc.createWorldOpenFlows().openWorld(WORLD_NAME, () -> {
                LOGGER.warn("[Testing] Failed to open test world");
                triggered = false;
            });
        } else {
            LOGGER.info("[Testing] Creating new flat test world '{}'", WORLD_NAME);
            LevelSettings settings = new LevelSettings(
                    WORLD_NAME,
                    GameType.CREATIVE,
                    false, // not hardcore
                    Difficulty.PEACEFUL,
                    true, // allow commands (cheats)
                    new GameRules(FeatureFlags.DEFAULT_FLAGS),
                    WorldDataConfiguration.DEFAULT
            );
            WorldOptions worldOptions = new WorldOptions(0L, false, false); // seed 0, no structures, no bonus chest
            mc.createWorldOpenFlows().createFreshLevel(
                    settings.levelName(),
                    settings,
                    worldOptions,
                    WorldPresets::createFlatWorldDimensions,
                    mc.screen
            );
        }
    }
}

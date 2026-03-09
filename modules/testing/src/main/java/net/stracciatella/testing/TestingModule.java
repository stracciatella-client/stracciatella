package net.stracciatella.testing;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.stracciatella.module.Module;
import net.stracciatella.testing.example.CommandTests;
import net.stracciatella.testing.example.ParkourTests;
import net.stracciatella.testing.runner.TestRunner;

public class TestingModule implements Module {
    @Task(lifeCycle = LifeCycle.STARTED)
    private void started() {
        // Register example test suites
        TestRunner.instance().registerSuite(ParkourTests.class);
        TestRunner.instance().registerSuite(CommandTests.class);

        // Register the /stracciatella-test client command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("stracciatella-test").executes(context -> {
                if (TestRunner.instance().isRunning()) {
                    context.getSource().sendFeedback(Component.literal("[Testing] Tests are already running."));
                    return 0;
                }
                context.getSource().sendFeedback(Component.literal("[Testing] Starting test runner..."));
                TestRunner.instance().start(() -> {
                    var results = TestRunner.instance().results();
                    long passed = results.stream().filter(r -> r.status() == net.stracciatella.testing.runner.TestResult.Status.PASSED).count();
                    long failed = results.size() - passed;
                    context.getSource().sendFeedback(Component.literal(
                            "[Testing] Done. " + passed + " passed, " + failed + " failed. See log for details."
                    ));
                });
                return Command.SINGLE_SUCCESS;
            }));
        });

        logger().info("Testing framework loaded. Use /stracciatella-test in-game to run tests.");
    }
}

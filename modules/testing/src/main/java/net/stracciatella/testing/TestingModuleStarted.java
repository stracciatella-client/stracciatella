package net.stracciatella.testing;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.stracciatella.testing.runner.TestResult;
import net.stracciatella.testing.runner.TestRunner;

public class TestingModuleStarted {
    public TestingModuleStarted() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("stracciatella-test").executes(context -> {
                if (TestRunner.instance().isRunning()) {
                    context.getSource().sendFeedback(Component.literal("[Testing] Tests are already running."));
                    return 0;
                }
                context.getSource().sendFeedback(Component.literal("[Testing] Starting test runner..."));
                TestRunner.instance().start(() -> {
                    var results = TestRunner.instance().results();
                    long passed = results.stream().filter(r -> r.status() == TestResult.Status.PASSED).count();
                    long failed = results.size() - passed;
                    Minecraft.getInstance().execute(() -> {
                        context.getSource().sendFeedback(Component.literal(
                                "[Testing] Done. " + passed + " passed, " + failed + " failed. See log for details."
                        ));
                    });
                });
                return Command.SINGLE_SUCCESS;
            }));
        });
    }
}

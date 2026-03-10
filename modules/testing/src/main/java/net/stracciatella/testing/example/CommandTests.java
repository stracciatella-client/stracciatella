package net.stracciatella.testing.example;

import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.command.ChatInterceptor;
import net.stracciatella.testing.command.CommandExecutor;

/**
 * Example command test suite. Demonstrates how to execute commands
 * and assert on chat message responses.
 */
@TestSuite(name = "Command Tests")
public class CommandTests {

    @MinecraftTest(name = "Seed command returns seed", timeoutTicks = 100, order = 1, repeat = 5)
    public void seedCommand(TestContext ctx) {
        ChatInterceptor interceptor = ChatInterceptor.instance();
        interceptor.clear();

        // Listen for the response
        interceptor.setListener(message -> {
            if (message.contains("Seed:") || message.contains("[")) {
                interceptor.clearListener();
                ctx.complete();
            }
        });

        CommandExecutor.executeCommand("seed");
    }

    @MinecraftTest(name = "Time query returns daytime", timeoutTicks = 100, order = 2, repeat = 5)
    public void timeQuery(TestContext ctx) {
        ChatInterceptor interceptor = ChatInterceptor.instance();
        interceptor.clear();

        interceptor.setListener(message -> {
            // Time query responds with "The time is ..."
            if (message.toLowerCase().contains("time")) {
                interceptor.clearListener();
                ctx.complete();
            }
        });

        CommandExecutor.executeCommand("time query daytime");
    }
}

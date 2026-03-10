package net.stracciatella.testing.example;

import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.command.ChatInterceptor;

/**
 * Example command test suite. Demonstrates how to execute commands
 * and assert on chat message responses using the blocking wait model.
 */
@TestSuite(name = "Command Tests")
public class CommandTests {

    @MinecraftTest(name = "Seed command returns seed", timeoutTicks = 100, order = 1, repeat = 5)
    public void seedCommand(TestContext ctx) {
        ChatInterceptor.instance().clear();
        ctx.runCommand("seed");
        ctx.waitFor(mc -> ChatInterceptor.instance().hasMessageContaining("Seed:")
                || ChatInterceptor.instance().hasMessageContaining("["));
    }

    @MinecraftTest(name = "Time query returns daytime", timeoutTicks = 100, order = 2, repeat = 5)
    public void timeQuery(TestContext ctx) {
        ChatInterceptor.instance().clear();
        ctx.runCommand("time query daytime");
        ctx.waitFor(mc -> {
            String lower = String.join(" ", ChatInterceptor.instance().messages()).toLowerCase();
            return lower.contains("time");
        });
    }
}

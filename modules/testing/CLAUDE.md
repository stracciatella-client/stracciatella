# Testing Module

Client-side integration testing framework built on the Fabric Client GameTest API.

## Architecture

```
net.stracciatella.testing
├── TestingModule.java           # Entry point. Registers test suites
├── GameTestEntrypoint.java      # FabricClientGameTest impl. Creates world, runs tests
├── api/                         # Public API for writing tests
│   ├── @MinecraftTest           # Annotate test methods (params: name, timeoutTicks, order, repeat)
│   ├── @TestSuite               # Annotate test classes (params: name)
│   └── TestContext              # Wraps ClientGameTestContext. Provides waitFor/runOnClient/runCommand
├── runner/                      # Test execution engine
│   ├── TestRunner               # Singleton. Discovers suites, runs sequentially via runAll()
│   ├── RegisteredTest           # Internal record wrapping a discovered test method
│   └── TestResult               # Record: suiteName, testName, status, message, durationMs
├── movement/                    # Player movement utilities
│   ├── MovementController       # Static methods using TestInput: holdKey/releaseKey, lookAt
│   └── MovementTracker          # Records positions per tick, checks falls/distance/arrival
├── command/                     # Command execution utilities
│   ├── CommandExecutor          # Static: executeCommand(string), sendChat(string)
│   └── ChatInterceptor          # Singleton. Captures system messages, supports listeners
├── mixin/                       # Hooks into Minecraft
│   └── ChatListenerMixin        # ChatListener.handleSystemMessage() → ChatInterceptor
└── example/                     # Example test suites
    └── CommandTests             # /seed, /time assertions using waitFor + ChatInterceptor
```

## How tests work

1. Tests are registered during module init via `TestRunner.instance().registerSuite(Class)`
2. Triggered by `./gradlew runMinecraftTests` (sets `-Dfabric-api.client-gametest`)
3. `GameTestEntrypoint.runTest()` creates a flat creative world, then calls `TestRunner.runAll()`
4. TestRunner runs tests one at a time, passing a `TestContext` wrapping `ClientGameTestContext`
5. Tests are **synchronous blocking code** — use `ctx.waitFor()`, `ctx.waitTick()`, `ctx.runOnClient()`
6. Success = method returns normally. Failure = throw any exception (AssertionError, etc.)
7. Timeout is managed per-test via `@MinecraftTest.timeoutTicks`

## Key conventions

- Test methods must have signature `void methodName(TestContext ctx)`
- Suite classes need a public no-arg constructor
- Use `ctx.waitFor(predicate)` to block until a condition is met (replaces old TickHandler pattern)
- Use `ctx.runOnClient(action)` to run code on the render thread
- Use `ctx.runCommand(cmd)` to execute commands (sends + waits one tick)
- Use `ctx.input()` to get TestInput for key simulation (holdKey, releaseKey)
- `ctx.fail(reason)` throws AssertionError to fail immediately
- ChatInterceptor still needs ChatListenerMixin to capture messages

## How to add a new test suite

1. Create a class annotated with `@TestSuite`
2. Add `@MinecraftTest` methods that accept `TestContext`
3. Use blocking waits instead of tick callbacks:
   ```java
   ctx.runCommand("tp @s 0 100 0");
   ctx.waitFor(mc -> mc.player.onGround());
   ```
4. Register in module init: `TestRunner.instance().registerSuite(YourSuite.class)`

## Build & run

- Build: `./gradlew :modules:testing:build -x checkstyleMain`
- Run tests: `./gradlew runMinecraftTests`
- Module registered in settings.gradle.kts and modules/build.gradle.kts
- Entrypoint injected into generated fabric.mod.json via build.gradle.kts

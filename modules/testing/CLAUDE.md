# Testing Module

Client-side integration testing framework that runs inside a live Minecraft instance.

## Architecture

```
net.stracciatella.testing
├── TestingModule.java           # Entry point. Registers suites + /stracciatella-test command
├── api/                         # Public API for writing tests
│   ├── @MinecraftTest           # Annotate test methods (params: name, timeoutTicks, order, repeat)
│   ├── @TestSuite               # Annotate test classes (params: name)
│   └── TestContext              # Blocking test API: waitFor, waitTick, runOnClient, runCommand
├── runner/                      # Test execution engine
│   ├── TestRunner               # Singleton. Discovers, runs on dedicated thread, reports results
│   ├── RegisteredTest           # Internal record wrapping a discovered test method
│   └── TestResult               # Record: suiteName, testName, status, message, durationMs
├── movement/                    # Player movement utilities
│   ├── MovementController       # Static methods: lookAt, pressForward/Sprint/Jump, teleport
│   └── MovementTracker          # Records positions per tick, checks falls/distance/arrival
├── command/                     # Command execution utilities
│   ├── CommandExecutor          # Static: executeCommand(string), sendChat(string)
│   └── ChatInterceptor          # Singleton. Captures system messages, supports listeners
├── mixin/                       # Hooks into Minecraft
│   ├── ClientTickMixin          # Minecraft.tick() tail → TestRunner.onClientTick()
│   ├── ChatListenerMixin        # ChatListener.handleSystemMessage() → ChatInterceptor
│   └── TitleScreenMixin         # TitleScreen.init() → AutoTestWorld (if autorun)
├── world/                       # Test world management
│   └── AutoTestWorld            # Creates/joins flat creative test world for autorun
└── example/                     # Example test suites
    └── CommandTests             # /seed, /time assertions using waitFor + ChatInterceptor
```

## How tests work

1. Tests are registered during module init via `TestRunner.instance().registerSuite(Class)`
2. Triggered by `/stracciatella-test` in-game or `-Dstracciatella.testing.autorun=true`
3. TestRunner spawns a dedicated test thread and runs tests sequentially
4. Each test method receives a `TestContext` with blocking utilities
5. Tests are **synchronous blocking code** — use `ctx.waitFor()`, `ctx.waitTick()`, `ctx.runOnClient()`
6. Success = method returns normally. Failure = throw any exception (AssertionError, etc.)
7. Timeout is managed per-test via `@MinecraftTest.timeoutTicks` and `ctx.waitFor()` auto-timeout
8. ClientTickMixin signals the test thread each tick via `TestContext.onClientTick()`

## Key conventions

- Test methods must have signature `void methodName(TestContext ctx)`
- Suite classes need a public no-arg constructor
- Use `ctx.waitFor(predicate)` to block until a condition is met
- Use `ctx.runOnClient(action)` to run code on the render thread
- Use `ctx.runCommand(cmd)` to execute commands (sends + waits one tick)
- `ctx.fail(reason)` throws AssertionError to fail immediately
- `ctx.computeOnClient(fn)` to read game state from the test thread

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

- Build: `./gradlew :modules:testing:build`
- Run tests in-game: join world → `/stracciatella-test`
- Auto-run: `./gradlew runMinecraftTests` (sets autorun system property)

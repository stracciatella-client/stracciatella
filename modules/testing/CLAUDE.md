# Testing Module

Client-side integration testing framework that runs inside a live Minecraft instance.

## Architecture

```
net.stracciatella.testing
├── TestingModule.java           # Entry point. Registers suites + /stracciatella-test command
├── api/                         # Public API for writing tests
│   ├── @MinecraftTest           # Annotate test methods (params: name, timeoutTicks, order)
│   ├── @TestSuite               # Annotate test classes (params: name)
│   ├── TestContext              # Passed to test methods. Call complete()/fail() to finish
│   └── TickHandler              # Implement on suite class to receive onTick(ctx) each tick
├── runner/                      # Test execution engine
│   ├── TestRunner               # Singleton. Discovers, runs sequentially, reports results
│   ├── RegisteredTest           # Internal record wrapping a discovered test method
│   └── TestResult               # Record: suiteName, testName, status, message, durationTicks
├── movement/                    # Player movement utilities
│   ├── MovementController       # Static methods: lookAt, pressForward/Sprint/Jump, teleport
│   └── MovementTracker          # Records positions per tick, checks falls/distance/arrival
├── command/                     # Command execution utilities
│   ├── CommandExecutor          # Static: executeCommand(string), sendChat(string)
│   └── ChatInterceptor          # Singleton. Captures system messages, supports listeners
├── mixin/                       # Hooks into Minecraft
│   ├── ClientTickMixin          # Minecraft.tick() tail → TestRunner.onClientTick()
│   └── ChatListenerMixin        # ChatListener.handleSystemMessage() → ChatInterceptor
└── example/                     # Example test suites
    ├── ParkourTests             # Walk-to-target, sprint-jump (implements TickHandler)
    └── CommandTests             # /seed, /time, /gamemode assertions (uses ChatInterceptor)
```

## How tests work

1. Tests are registered in TestingModule.started() via `TestRunner.instance().registerSuite(Class)`
2. Triggered by `/stracciatella-test` in-game or `-Dstracciatella.testing.autorun=true` system prop
3. TestRunner runs tests one at a time, driven by ClientTickMixin each client tick
4. Each test method receives a TestContext and must call ctx.complete() or ctx.fail(reason)
5. Synchronous tests (commands): set a ChatInterceptor listener, call complete/fail in callback
6. Async tests (movement): implement TickHandler on the suite, track state across ticks
7. Tests time out after timeoutTicks (default 600 = 30 seconds)

## Key conventions

- Test methods must have signature `void methodName(TestContext ctx)`
- Suite classes need a public no-arg constructor
- TickHandler.onTick() is only called while the current test belongs to that suite instance
- MovementController simulates keyboard inputs (setDown on KeyMapping), not direct position changes
- ChatInterceptor.clear() resets both messages and listener — call before each command test
- Results logged at INFO (pass) or ERROR (fail) level with tick durations

## How to add a new test suite

1. Create a class annotated with `@TestSuite`
2. Add `@MinecraftTest` methods that accept `TestContext`
3. For tick-based tests, implement `TickHandler`
4. Register in `TestingModule.started()`: `TestRunner.instance().registerSuite(YourSuite.class)`

## Build & run

- Build: `./gradlew :modules:testing:build`
- Run tests: launch game → join world → `/stracciatella-test`
- Auto-run: `./gradlew runMinecraftTests` (sets autorun system property)
- Module registered in settings.gradle.kts and modules/build.gradle.kts

import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

repositories {
    val repos = this.toList()
    maven("https://reposilite.dasbabypixel.de/stracciatella") {
        name = "Stracciatella"
    }
    this.addAll(repos)
}

loom {
    mods {
        register("stracciatella-testing") {
            sourceSet(sourceSets.getByName(MAIN_SOURCE_SET_NAME))
        }
    }
}

stracciatella {
    main = "net.stracciatella.testing.TestingModule"
    id = "testing"
    name = "Testing Framework"
    group = "net.stracciatella"
    mixin("testing.mixins.json")
}

dependencies {
    compileOnly(projects.loader)
    implementation(libs.junit.jupiter)
}

// Convenience task — launches the game client with auto-test flag.
// Creates/joins a flat test world automatically, runs all tests, prints results.
// Usage: ./gradlew runMinecraftTests
gradle.taskGraph.whenReady {
    if (allTasks.any { it.name == "runMinecraftTests" }) {
        allTasks.filterIsInstance<JavaExec>().filter { it.name == "runStracciatellaLight" }.forEach {
            it.jvmArgs("-Dstracciatella.testing.autorun=true")
        }
    }
}

tasks.register("runMinecraftTests") {
    group = "verification"
    description = "Launches the Minecraft client with auto-test mode enabled"
    dependsOn(":runStracciatellaLight")
}

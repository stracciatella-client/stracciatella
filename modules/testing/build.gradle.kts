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
// Tests run automatically when the player joins a world.
tasks.register("runMinecraftTests") {
    group = "verification"
    description = "Launches the Minecraft client with auto-test mode enabled"
    dependsOn(":runClient")
    doFirst {
        System.setProperty("stracciatella.testing.autorun", "true")
    }
}

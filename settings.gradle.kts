enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
includeBuild("build-extensions")
rootProject.name = "stracciatella"
pluginManagement {
    repositories {
        maven("https://reposilite.dasbabypixel.de/stracciatella") {
            name = "Stracciatella"
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://reposilite.dasbabypixel.de/jitpack") { name = "Jitpack-Mirror" }
    }
}
dependencyResolutionManagement {
    versionCatalogs {
        register("mods") {
            from(files("mods.versions.toml"))
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("loader")
include("loader:injected")
include("loader:test3module")
includeModule("core")
includeModule("fullscreen")
includeModule("anonymous-modlist")
includeModule("camera")
includeModule("pathfinding")
includeModule("testing")

fun includeModule(path: String) {
    include("modules:$path")
}
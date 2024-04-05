enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
includeBuild("build-extensions")
rootProject.name = "stracciatella"
pluginManagement {
    repositories {
        mavenLocal()
        mavenLocal {
            content {
                includeGroup("net.fabricmc")
            }
        }
        maven("https://nexus.darkcube.eu/repository/stracciatella/") { name = "Stracciatella" }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://nexus.darkcube.eu/repository/jitpack/") { name = "Jitpack-Mirror" }
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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("loader")
include("loader:injected")
include("loader:test3module")
includeModule("core")
includeModule("fullscreen")

fun includeModule(path: String) {
    include("modules:$path")
}
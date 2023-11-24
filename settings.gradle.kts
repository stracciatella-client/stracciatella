enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
includeBuild("build-extensions")
rootProject.name = "stracciatella"
pluginManagement {
    repositories {
        maven("https://nexus.darkcube.eu/repository/stracciatella/") { name = "Stracciatella" }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

include("loader")
include("loader:test3module")
includeModule("core")

fun includeModule(path: String) {
    include("modules:$path")
}
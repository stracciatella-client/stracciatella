plugins {
    `version-catalog`
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
    maven("https://nexus.darkcube.eu/repository/stracciatella") { name = "Stracciatella" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    api(libs.stracciatella)
    api("com.google.code.gson:gson:2.10.1")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("stracciatella-root") {
            id = "stracciatella-root"
            implementationClass = "stracciatella.root.StracciatellaRootPlugin"
        }
    }
}
plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    api("net.fabricmc:fabric-loom:1.4.1")
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
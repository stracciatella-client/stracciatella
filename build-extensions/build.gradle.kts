import org.jetbrains.kotlin.gradle.utils.extendsFrom

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

kotlin {
    jvmToolchain(17)
}

val curseApi = configurations.register("curseApi") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.compileClasspath.extendsFrom(curseApi)
dependencies {
    curseApi(project("curse-api-generator", "curseApi"))

    api(libs.stracciatella)
    api("dev.masecla:Modrinth4J:2.0.0")
    api("com.google.code.gson:gson:2.10.1")
}

sourceSets.main {
    java.srcDir(curseApi.get().singleFile)
}

gradlePlugin {
    plugins {
        register("stracciatella-root") {
            id = "stracciatella-root"
            implementationClass = "stracciatella.root.StracciatellaRootPlugin"
        }
    }
}

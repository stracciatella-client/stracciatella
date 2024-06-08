import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
    maven("https://nexus.darkcube.eu/repository/jitpack/") { name = "Jitpack-Mirror" }
}

kotlin {
    jvmToolchain(21)
}

val curseApi = configurations.register("curseApi") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.compileClasspath.extendsFrom(curseApi)
dependencies {
    curseApi(project("curse-api-generator", "curseApi"))

    api(libs.stracciatella)
    api("org.tomlj:tomlj:1.1.1")
    api("com.github.DasBabyPixel:Modrinth4J:bc79cbed95")
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

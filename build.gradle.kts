import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RunGameTask
import stracciatella.modlist.ModListCreator
import stracciatella.modlist.ModListGenerator

plugins {
    alias(libs.plugins.stracciatella.fabric) apply false
    id("fabric-loom") version "1.8.0-alpha.50001"
    alias(libs.plugins.stracciatella) apply false
    alias(libs.plugins.stracciatella.base)
    `version-catalog`
    id("stracciatella-root")
}

version = providers.gradleProperty("version").get()
group = providers.gradleProperty("group").get()

val modList = configurations.register("modlist")
configurations.modRuntimeOnly.configure { extendsFrom(modList.get()) }
val includeInCreator = configurations.register("modlist2")

dependencies {
    modList(mods.bundles.mods) { isTransitive = false }
    includeInCreator(projects.loader) { targetConfiguration = "finalJar" }
    implementation(projects.loader) { targetConfiguration = "mergedJar" }
    "stracciatellaModule"(projects.modules)
}

tasks {
    register<ModListGenerator>("generateModList") {
        setup("generateModList.toml")
    }
    register<ModListCreator>("createModList") {
        modFiles.from(modList)
        modFiles.from(includeInCreator)
    }
}

allprojects {
    tasks {
        withType<RunGameTask>().configureEach {
            maxHeapSize = "8G"
            workingDir(rootProject.projectDir)
        }
    }
    pluginManager.apply {
        withPlugin("fabric-loom") {
            extensions.findByType<LoomGradleExtensionAPI>()?.apply {
                dependencies {
                    "minecraft"(rootProject.libs.minecraft)
                    "mappings"(officialMojangMappings())
                    "modImplementation"(rootProject.libs.fabric.loader)

                    // Fabric API. This is technically optional, but you probably want it anyway.
                    "modImplementation"(rootProject.libs.fabric.api)

                    "testImplementation"(rootProject.libs.junit.jupiter)
                    "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
                }
            }
        }
        withPlugin("checkstyle") {
            extensions.findByType<CheckstyleExtension>()?.apply {
                toolVersion = rootProject.libs.versions.checkstyle.get()
            }
        }
    }
}

gradle.taskGraph.whenReady {
    allTasks.filterIsInstance<JavaExec>().forEach {
        it.executable(it.javaLauncher.get().executablePath.asFile.absolutePath)
    }
}
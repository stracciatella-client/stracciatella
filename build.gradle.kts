import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RunGameTask

plugins {
    alias(libs.plugins.stracciatella.fabric)
    alias(libs.plugins.stracciatella) apply false
    alias(libs.plugins.stracciatella.base)
    `version-catalog`
    id("stracciatella-root")
}

version = providers.gradleProperty("version").get()
group = providers.gradleProperty("group").get()

dependencies {
    modRuntimeOnly(mods.bundles.mods) { isTransitive = false }
    implementation(projects.loader) { targetConfiguration = "namedElements" }
    "stracciatellaModule"(projects.modules)
}

allprojects {
    tasks {
        withType<RunGameTask>().configureEach {
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

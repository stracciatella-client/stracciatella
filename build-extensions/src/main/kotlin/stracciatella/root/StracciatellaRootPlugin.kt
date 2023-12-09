package stracciatella.root

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RunGameTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

class StracciatellaRootPlugin : Plugin<Project> {
    companion object {
        const val MODULE_CONFIGURATION = "stracciatellaModule"
    }

    override fun apply(project: Project) {
        project.run {
            version = providers.gradleProperty("version").get()
            group = providers.gradleProperty("group").get()

            tasks.apply {
                val generateClasspath = register<GenerateClasspath>("stracciatellaGenerateClasspath")
                val runClient = named<RunGameTask>("runClient")
                registerStracciatellaTask(this, generateClasspath, runClient)
            }
            configurations.register(MODULE_CONFIGURATION)
            extensions.getByType<LoomGradleExtensionAPI>().apply {
                runConfigs.configureEach {
                    ideConfigGenerated(false)
                }
            }
        }

        project.allprojects {
            repositories.apply {
                maven("https://nexus.darkcube.eu/repository/stracciatella") {
                    name = "Stracciatella"
                }
                maven("https://maven.flashyreese.me/releases") // CaffeineConfig
                exclusiveContent {
                    forRepository {
                        maven("https://maven.shedaniel.me") {
                            name = "Shedaniel"
                        }
                    }
                    filter {
                        includeGroup("me.shedaniel.cloth")
                    }
                }
                exclusiveContent {
                    forRepository {
                        maven("https://api.modrinth.com/maven") {
                            name = "Modrinth"
                        }
                    }
                    filter {
                        includeGroup("maven.modrinth")
                    }
                }
                exclusiveContent {
                    forRepository {
                        maven("https://cursemaven.com") {
                            name = "Cursemaven"
                        }
                    }
                    filter {
                        includeGroup("curse.maven")
                    }
                }
                exclusiveContent {
                    forRepository {
                        maven("https://maven.gegy.dev") {
                            name = "spruceui"
                        }
                    }
                    filter {
                        includeGroup("dev.lambdaurora")
                    }
                }
                exclusiveContent {
                    forRepository {
                        maven("https://maven.meteordev.org/snapshots") {
                            name = "meteor"
                        }
                    }
                    filter {
                        includeGroup("meteordevelopment")
                    }
                }
            }
            tasks {
                val libraries = layout.buildDirectory.dir("stracciatella").map { it.dir("libraries") }
                withType<Test>().configureEach {
                    useJUnitPlatform()
                    inputs.files(libraries)
                    jvmArgs("-DstracciatellaLibraryStorage=${libraries.get().asFile.canonicalPath}")
                }
            }
        }

        project.subprojects {
            version = project.version
            group = project.group
        }
    }

    private fun registerStracciatellaTask(tasks: TaskContainer, generateClasspath: TaskProvider<GenerateClasspath>, runClient: TaskProvider<RunGameTask>) {
        tasks.register("stracciatella") {
            dependsOn(generateClasspath)
            doFirst {
                val outputFile = generateClasspath.get().compiledOutput
                runClient.configure {
                    jvmArgs("-DstracciatellaClasspath=${outputFile.get().asFile.canonicalPath}")
                }
            }
            finalizedBy(runClient)
        }
    }
}
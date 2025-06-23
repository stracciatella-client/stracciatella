package stracciatella.root

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RunGameTask
import net.fabricmc.loom.util.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
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

            configurations.register(MODULE_CONFIGURATION)
            val sourceSets = extensions.getByType<SourceSetContainer>()
            val sourceLight = sourceSets.register("light")
            val source = sourceSets.register("full")

            extensions.getByType<LoomGradleExtensionAPI>().apply {
                createRemapConfigurations(sourceLight.get())
                createRemapConfigurations(source.get())
                runs {
                    val light = this.register("stracciatellaLight") {
                        inherit(getByName("client"))
                        this.source(sourceLight.get())
                    }
                    this.register("stracciatella") {
                        this.inherit(light.get())
                        this.source(source.get())
                    }
                }
                runConfigs.configureEach {
                    ideConfigGenerated(false)
                }
            }

            tasks.apply {
                val generateClasspath = register<GenerateClasspath>("stracciatellaGenerateClasspath")
                val runStracciatella = named<RunGameTask>("runStracciatella")
                val runStracciatellaLight = named<RunGameTask>("runStracciatellaLight")
                registerStracciatellaTask("stracciatella", this, generateClasspath, runStracciatella)
                registerStracciatellaTask("stracciatellaLight", this, generateClasspath, runStracciatellaLight)
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

    private fun registerStracciatellaTask(
        name: String,
        tasks: TaskContainer,
        generateClasspath: TaskProvider<GenerateClasspath>,
        runClient: TaskProvider<RunGameTask>
    ) {
        runClient.configure {
            dependsOn(generateClasspath)
            doFirst {
                val outputFile = generateClasspath.get().compiledOutput
                jvmArgs("-DstracciatellaClasspath=${outputFile.get().asFile.canonicalPath}")
            }
        }
    }
}
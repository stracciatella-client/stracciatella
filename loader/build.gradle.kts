import net.stracciatella.gradle.plugin.SourceSetDependency
import net.stracciatella.gradle.plugin.StracciatellaExtension

plugins {
    id(libs.plugins.stracciatella.fabric.get().pluginId)
    alias(libs.plugins.shadow)
}

val testModuleNames = listOf("test1module", "test2module")

repositories {
    val repos = this.toList()
    maven("https://reposilite.dasbabypixel.de/stracciatella") {
        name = "Stracciatella"
    }
    this.addAll(repos)
}

sourceSets {
    for (testModuleName in testModuleNames) {
        register(testModuleName) {
            java.srcDir("src/${testModuleName}/java")
            resources.srcDir("src/${testModuleName}/java")
        }.run {
            StracciatellaExtension.registerGeneratorAndConfigurations(project, testModuleName) {
                val nr = testModuleName.replace("test", "").replace("module", "").toInt()
                id = testModuleName
                name = "TestModule$nr"
                main = "net.stracciatella.test.${testModuleName}.$name"
            }
        }
    }
}

configurations {
    val injected = register("injected")
    compileOnly.configure {
        extendsFrom(injected.get())
    }
    resolvable("includeInJar") {
        extendsFrom(include.get())
    }
    consumable("mergedJar")
}

dependencies {
    for (testModuleName in testModuleNames) {
        "${testModuleName}Implementation"(project)
    }
    modApi(libs.fabric.api)
    include(libs.jol.core)
    implementation(libs.jol.core)
    "injected"(project("injected"))
    // TODO
    "test1moduleStracciatellaDependency"(project("test3module"))
    "test2moduleStracciatellaDependency"(sourceSets.named("test1module").map { SourceSetDependency(it) })
    "test1moduleStracciatellaLibrary"("de.dasbabypixel:annotations:0.1")
    "test1moduleStracciatellaLibrary"("de.dasbabypixel:utils:1.0")

    "test2moduleCompileOnly"("de.dasbabypixel:annotations:0.1")
    "test2moduleCompileOnly"("de.dasbabypixel:utils:1.0")
}

tasks {
    val testModules = ArrayList<TaskProvider<Jar>>()
    for (testModuleName in testModuleNames) {
        testModules.add(register<Jar>("${testModuleName}Jar") {
            dependsOn(named("${testModuleName}Classes"))
            from(sourceSets.named(testModuleName).map { it.output })
            from(named("generateStracciatella${testModuleName}ModuleJson"))
            destinationDirectory = project.layout.buildDirectory.dir("test")
            archiveFileName = "${testModuleName}.jar"
        })
    }
    processResources.configure {
        inputs.property("version", project.version.toString())
        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version.toString()))
        }
    }
    jar.configure {
        from(configurations.named("injected")) {
            rename { "injected.jar" }
        }
    }
    shadowJar.configure {
        archiveClassifier.convention("shadow")
        configurations = listOf(project.configurations["includeInJar"])
        destinationDirectory.convention(jar.flatMap { it.destinationDirectory })
    }
    remapJar.configure {
        this.archiveBaseName = "stracciatella"
    }
    val mergeJar = register<Jar>("mergeJar") {
        destinationDirectory.convention(jar.flatMap { it.destinationDirectory })
        archiveClassifier.convention("merged")
        from(shadowJar.map { it.outputs.files.map { it2 -> zipTree(it2) } })
        from(configurations.named("injected")) {
            rename { "injected.jar" }
        }
    }
    artifacts.add("mergedJar", mergeJar)
    project("test3module").afterEvaluate {
        val test3moduleJar = this.tasks.named<AbstractArchiveTask>("remapJar")
        val classpath = ArrayList<String>()
        testModules.forEach { classpath.add(it.get().archiveFile.get().asFile.canonicalPath) }
        classpath.add(test3moduleJar.get().archiveFile.get().asFile.canonicalPath)
        this@tasks.named<Test>("test") {
            inputs.files(testModules)
            inputs.files(test3moduleJar)
            systemProperty(
                "stracciatellaClasspath",
                classpath.joinToString(separator = File.pathSeparator)
            )
        }
    }
}

configurations.consumable("finalJar") {
    outgoing.artifact(tasks.remapJar) {
        name = "stracciatella-loader"
    }
}

loom {
    accessWidenerPath = file("src/main/resources/stracciatella.accesswidener")
    mods {
        register("stracciatella") {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}

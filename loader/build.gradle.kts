import net.stracciatella.gradle.plugin.SourceSetDependency
import net.stracciatella.gradle.plugin.StracciatellaExtension

plugins {
    id(libs.plugins.stracciatella.fabric.get().pluginId)
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val testModuleNames = listOf("test1module", "test2module")

sourceSets {
    for (testModuleName in testModuleNames) {
        register(testModuleName) {
            java.srcDir("src/${testModuleName}/java")
            resources.srcDir("src/${testModuleName}/java")
        }.run {
            StracciatellaExtension.registerGenerator(project, this@run) {
                name = "TestModule${
                    testModuleName.replace("test", "").replace("module", "")
                }"
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
    register("mergedJar") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

dependencies {
    for (testModuleName in testModuleNames) {
        "${testModuleName}Implementation"(project)
    }
    include(libs.jol.core)
    implementation(libs.jol.core)
    "injected"(project("injected"))
    "test1moduleStracciatellaDependency"(project("test3module"))
    "test2moduleStracciatellaDependency"(sourceSets.named("test1module").map { SourceSetDependency(it) })
    "test1moduleStracciatellaLibrary"("de.dasbabypixel:annotations:0.1")
    "test1moduleStracciatellaLibrary"("de.dasbabypixel:utils:1.0")
}

tasks {
    val testModules = ArrayList<TaskProvider<Jar>>()
    for (testModuleName in testModuleNames) {
        testModules.add(register<Jar>("${testModuleName}Jar") {
            dependsOn(named("${testModuleName}Classes"))
            from(sourceSets.named(testModuleName).map { it.output })
            destinationDirectory = project.layout.buildDirectory.dir("test")
            archiveFileName = "${testModuleName}.jar"
        })
    }
    jar.configure {
        from(configurations.named("injected")) {
            rename { "injected.jar" }
        }
    }
    shadowJar.configure {
        archiveClassifier.convention("shadow")
        configurations = listOf(project.configurations.include.get())
        destinationDirectory.convention(jar.flatMap { it.destinationDirectory })
    }
    val mergeJar = register<Jar>("mergeJar") {
        destinationDirectory.convention(jar.flatMap { it.destinationDirectory })
        archiveClassifier.convention("merged")
        from(shadowJar.map { it.outputs.files.map(::zipTree) })
        from(configurations.named("injected")) {
            rename { "injected.jar" }
        }
    }
    artifacts.add("mergedJar", mergeJar)
    remapJar.configure {
//        inputFile = jar.flatMap { it.archiveFile }
//        setDependsOn(emptyList<Task>())
    }
    project("test3module").afterEvaluate {
        val test3moduleJar = this.tasks.named<Jar>("jar")
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

loom {
    accessWidenerPath = file("src/main/resources/stracciatella.accesswidener")
    mods {
        register("stracciatella") {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}

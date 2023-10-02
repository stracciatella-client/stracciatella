import net.fabricmc.loom.task.RunGameTask
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

plugins {
    id("fabric-loom") version "1.4-SNAPSHOT"
    id("token-replacement")
}

version = providers.gradleProperty("version").get()
group = providers.gradleProperty("group").get()
val modid = providers.gradleProperty("modid").get()

base {
    archivesName = providers.gradleProperty("archives_base_name")
}

tokens {
    replace("\$project.modid", modid)
    replace("\$project.version", version)
    excludeFileRegex(".*\\.png")
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

loom {
    accessWidenerPath = file("src/main/resources/stracciatella.accesswidener")
    mods {
        register(modid) {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}
tasks.named<RunGameTask>("runClient") {
    this.javaLauncher = javaToolchains.launcherFor(java.toolchain)
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation(libs.fabric.api)
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

allprojects {
    extensions.getByType<JavaPluginExtension>().apply {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.ORACLE
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

//jar {
//	from("LICENSE") {
//		rename { "${it}_${project.base.archivesName.get()}"}
//	}
//}

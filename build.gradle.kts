plugins {
    id("fabric-loom") version "1.4-SNAPSHOT"
    id("token-replacement")
    checkstyle
}

version = providers.gradleProperty("version").get()
group = providers.gradleProperty("group").get()
val modid: String = providers.gradleProperty("modid").get()

dependencies {
    // To change the versions see the gradle.properties file
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation(libs.fabric.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

repositories {
}

tokens {
    replace("\$project.modid", modid)
    replace("\$project.version", version)
    excludeFileRegex(".*\\.png")
}

loom {
    accessWidenerPath = file("src/main/resources/stracciatella.accesswidener")
    mods {
        register(modid) {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}

java {
    withSourcesJar()
}

allprojects {
    extensions.apply {
        findByType<JavaPluginExtension>()?.apply {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
                vendor = JvmVendorSpec.ORACLE
            }
        }
        findByType<CheckstyleExtension>()?.apply {
            toolVersion = libs.versions.checkstyle.get()
        }
    }
    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }
        withType<JavaExec> {
            javaLauncher = project.javaToolchains.launcherFor(project.java.toolchain)
        }
        withType<Checkstyle> {
            javaLauncher = project.javaToolchains.launcherFor(project.java.toolchain)
            maxErrors = 0
            maxWarnings = 0
            configFile = rootProject.file("stracciatella_checks.xml")
        }
    }
}

import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

repositories {
    val repos = this.toList()
    maven("https://reposilite.dasbabypixel.de/stracciatella") {
        name = "Stracciatella"
    }
    this.addAll(repos)
}

loom {
    mods {
        register("pathfinding") {
            sourceSet(sourceSets.getByName(MAIN_SOURCE_SET_NAME))
        }
    }
    this.mixin {
        this.useLegacyMixinAp
    }
}
tasks.checkstyleMain {
    this.maxWarnings = 100
}

stracciatella {
    main = "net.stracciatella.pathfinding.PathfindingModule"
    id = "pathfinding"
    name = "Pathfinding"
    group = "net.stracciatella"
    mixin("pathfinding.mixins.json")
    accessWidener("pathfinding.accesswidener")
}

dependencies {
    compileOnly(projects.loader)
    compileOnly(project(":modules:camera"))
    compileOnly(project(":modules:testing"))
    modCompileOnly(mods.sodium)
}

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
//    mixin("fullscreen.mixins.json")
//    accessWidener("fullscreen.accesswidener")
}

dependencies {
    compileOnly(projects.loader)
    modCompileOnly(mods.sodium)
}

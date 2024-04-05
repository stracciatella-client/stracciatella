import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

loom {
    mods {
        register("stracciatella-fullscreen") {
            sourceSet(sourceSets.getByName(MAIN_SOURCE_SET_NAME))
        }
    }
}

stracciatella {
    main = "net.stracciatella.fullscreen.FullscreenModule"
    id = "core"
    name = "Borderless Fullscreen"
    mixin("fullscreen.mixins.json")
    accessWidener("fullscreen.accesswidener")
}

dependencies {
    compileOnly(projects.loader)
    modCompileOnly(mods.sodium)
}
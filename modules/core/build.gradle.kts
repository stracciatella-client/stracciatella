import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

loom {
    mods {
        register("stracciatella-core") {
            sourceSet(sourceSets.getByName(MAIN_SOURCE_SET_NAME))
        }
    }
}

dependencies {
    stracciatellaLibrary("de.dasbabypixel:utils:1.0")
}

stracciatella {
    main = "net.stracciatella.core.CoreModule"
    id = "core"
    name = "Core Module"
    group = "net.stracciatella"
    mixin("core.mixins.json")
    accessWidener("core.accesswidener")
}

dependencies {
    compileOnly(projects.loader)
}
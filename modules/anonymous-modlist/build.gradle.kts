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
    this.mixin {
        this.useLegacyMixinAp
    }
}

stracciatella {
    main = "net.stracciatella.anonymousmodlist.AnonymousModlistModule"
    id = "anonymous-modlist"
    name = "Anonymous Modlist"
    group = "net.stracciatella"
    mixin("anonymous.mixins.json")
    //accessWidener("fullscreen.accesswidener")
}

dependencies {
    compileOnly(projects.loader)
    modCompileOnly(mods.sodium)
}
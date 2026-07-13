configurations.register("default") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

fun module(path: String) {
    dependencies {
        "default"(project(path))
    }
}
// declare all modules here
module("core")
module("fullscreen")
module("anonymous-modlist")

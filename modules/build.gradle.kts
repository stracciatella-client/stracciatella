configurations.register("default") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

fun module(path: String) {
    dependencies {
        "default"(project(path, "namedElements"))
    }
}
// declare all modules here
module("core")
module("fullscreen")

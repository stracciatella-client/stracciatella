plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

dependencies {
    minecraft(rootProject.libs.minecraft)
    implementation(rootProject.libs.fabric.loader)
    implementation(projects.loader)
}


repositories {
    val repos = this.toList()
    maven("https://reposilite.dasbabypixel.de/stracciatella") {
        name = "Stracciatella"
    }
    this.addAll(repos)
}

version = "1.0.1"

stracciatella {
    name = "TestModule3"
    id = "test3module"
    group = "net.stracciatella"
    main = "net.stracciatella.test.test3module.TestModule3"
    accessWidener("test.classtweaker")
    accessWidener("test2.classtweaker")
}

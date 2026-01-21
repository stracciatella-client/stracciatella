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

dependencies {
    implementation(projects.loader)
}

version = "1.0.1"

stracciatella {
    name = "TestModule3"
    id = "test3module"
    group = "net.stracciatella"
    main = "net.stracciatella.test.test3module.TestModule3"
}
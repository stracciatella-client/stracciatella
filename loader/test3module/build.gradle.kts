plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

dependencies {
    implementation(projects.loader)
}

version = "1.0.1"

stracciatella {
    name = "TestModule3"
    id = "test3module"
    main = "net.stracciatella.test.test3module.TestModule3"
}
plugins {
    id(libs.plugins.stracciatella.asProvider().get().pluginId)
}

stracciatella {
    main = "net.stracciatella.camera.CameraModule"
    id = "camera"
    name = "Camera"
    group = "net.stracciatella"
}

dependencies {
    compileOnly(projects.loader)
}

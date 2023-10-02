plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("tokenReplacement") {
            id = "token-replacement"
            implementationClass = "TokenReplacementPlugin"
        }
    }
}
plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("tokenReplacement") {
            id = "token-replacement"
            implementationClass = "TokenReplacementPlugin"
        }
    }
}
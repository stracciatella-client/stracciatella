plugins {
    kotlin("jvm")
//    java
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("com.google.code.gson:gson:2.11.0")
}

val generateCurseAPI = tasks.register<JavaExec>("generateCurseAPI") {
    mainClass = "net.stracciatella.generator.CurseApiGeneratorKt"
    classpath(java.sourceSets.main.map { it.runtimeClasspath })
    workingDir(layout.buildDirectory.dir("generated/sources/${this.name}/stracciatella/curse"))
    doFirst {
        workingDir.mkdirs()
    }
}

val curseApi = configurations.register("curseApi") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("curseApi", generateCurseAPI.get().workingDir.parentFile.parentFile) {
        builtBy(generateCurseAPI)
    }
}
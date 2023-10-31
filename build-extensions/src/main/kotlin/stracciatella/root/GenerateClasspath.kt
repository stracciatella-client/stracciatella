package stracciatella.root

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files

abstract class GenerateClasspath : DefaultTask() {
    @Classpath
    @InputFiles
    val classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputFile
    val compiledOutput = project.objects.fileProperty().convention(project.layout.buildDirectory.file("stracciatella/classpath"))

    init {
        val configuration = project.configurations.named(StracciatellaRootPlugin.MODULE_CONFIGURATION)
        classpath.from(configuration)
    }

    @TaskAction
    fun run() {
        val path = compiledOutput.get().asFile.toPath()
        val data = classpath.files.joinToString(separator = File.pathSeparator) { it.canonicalPath }
        Files.writeString(path, data)
    }
}
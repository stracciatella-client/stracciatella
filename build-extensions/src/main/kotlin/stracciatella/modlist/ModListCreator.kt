package stracciatella.modlist

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class ModListCreator : DefaultTask() {
    @InputFiles
    val modFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputDirectory
    val targetDirectory: DirectoryProperty = project.objects.directoryProperty()

    init {
        targetDirectory.convention(project.layout.buildDirectory.dir("modlist"))
    }

    @TaskAction
    fun download() {
        val dir = targetDirectory.asFile.get()
        dir.deleteRecursively()
        dir.mkdirs()
        modFiles.forEach {
            println("Downloaded $it")
            it.copyTo(dir.resolve(it.name))
        }
    }
}
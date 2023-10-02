import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.work.InputChanges
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import kotlin.io.path.name
import kotlin.io.path.pathString

@CacheableTask
abstract class TokenReplacement : DefaultTask() {

    @get:Inject
    internal abstract val objects: ObjectFactory

    @get: Inject
    internal abstract val fileSystemOperations: FileSystemOperations

    private val loggingReplacements = HashMap<String, String>()

    @Nested
    val sources = LinkedSourceDirectories()

    @Input
    val rules: MutableCollection<TokenReplacementRule> = ArrayList()

    @Input
    val excludeFiles: MutableCollection<SerializableRegex> = ArrayList()

    @OutputDirectory
    val output = objects.directoryProperty()

    @TaskAction
    fun run(inputChanges: InputChanges) {
        sources.forEach { node ->
            val name = node.tree.dir.path
            loggingReplacements[output.asFile.get().path] = name
            if (!Files.exists(node.tree.dir.toPath())) return@forEach
            inputChanges.getFileChanges(node.files).forEach {
                handleChange(it, node)
            }
        }
    }

    private fun handleChange(it: FileChange, node: LinkedSourceDirectories.Node) {
        val file = it.file.toPath()
        val relative = node.tree.dir.toPath().relativize(file)
        val dest = output.file(relative.toString()).get().asFile.toPath()
        for (excludeFile in excludeFiles) {
            if (excludeFile.compiled().matches(file.pathString)) {
                if (Files.isDirectory(file)) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    Files.copy(file, dest)
                }
                return
            }
        }
        rewrite(file, dest, it.changeType)
    }

    fun input(sourceDirectorySet: SourceDirectorySet) {
        for (srcDirTree in sourceDirectorySet.srcDirTrees) {
            sources.add(project.files(srcDirTree), srcDirTree, srcDirTree.patterns.includes, srcDirTree.patterns.excludes)
        }
    }

    private fun work(file: Path, dest: Path) {
        if (Files.isDirectory(file)) return
        logger.info("Rewrite " + file.name)
        var content: String = Files.readString(file)
        for (rule: TokenReplacementRule in rules) {
            content = content.replace(rule.regex.compiled(), rule.value)
        }
        Files.writeString(dest, content)
    }

    private fun rewrite(file: Path, dest: Path, change: ChangeType) {
        when (change) {
            ChangeType.REMOVED -> {
                if (Files.isDirectory(dest))
                    Files.walkFileTree(dest, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    })
                else
                    Files.deleteIfExists(dest)
            }

            ChangeType.ADDED, ChangeType.MODIFIED -> {
                if (Files.isRegularFile(file)) {
                    Files.createDirectories(dest.parent)
                    work(file, dest)
                } else if (Files.isDirectory(file)) {
                    Files.createDirectories(dest)
                }
            }
        }
    }
}
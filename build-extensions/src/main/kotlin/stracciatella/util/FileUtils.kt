package stracciatella.util

import java.nio.file.Files
import java.nio.file.Path

class FileUtils {
    companion object {
        fun delete(path: Path) {
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, DeleteDirectoryVisitor())
            } else {
                Files.deleteIfExists(path)
            }
        }
    }
}
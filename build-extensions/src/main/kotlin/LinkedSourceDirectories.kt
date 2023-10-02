import org.gradle.api.file.DirectoryTree
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import java.util.function.Consumer

class LinkedSourceDirectories {

    @Optional
    @Nested
    private var head: Node? = null

    fun add(files: FileCollection, tree: DirectoryTree, includes: MutableSet<String>, excludes: MutableSet<String>) {
        val node = Node(files, tree, includes, excludes)
        if (head == null) head = node
        else last()!!.next = node
    }

    fun forEach(consumer: Consumer<Node>) {
        var f = first()
        while (f != null) {
            consumer.accept(f)
            f = f.next
        }
    }

    fun getHead(): Node? {
        return head
    }

    fun first(): Node? {
        return head
    }

    fun last(): Node? {
        var cur: Node? = head
        var last: Node? = cur
        while (cur != null) {
            last = cur
            cur = cur.next
        }

        return last
    }

    class Node(
            @Incremental @InputFiles @PathSensitive(PathSensitivity.RELATIVE) val files: FileCollection, @Internal val tree: DirectoryTree, @Input val includes: MutableSet<String>, @Input val excludes: MutableSet<String>, @Nested @Optional var next: Node? = null
    )

}
package stracciatella.modlist

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import masecla.modrinth4j.client.agent.UserAgent
import masecla.modrinth4j.client.instances.RatelimitedHttpClient
import masecla.modrinth4j.endpoints.generic.empty.EmptyRequest
import masecla.modrinth4j.endpoints.version.GetProjectVersions.GetProjectVersionsRequest
import masecla.modrinth4j.main.ModrinthAPI
import masecla.modrinth4j.model.adapters.ISOTimeAdapter
import masecla.modrinth4j.model.project.Project
import masecla.modrinth4j.model.search.FacetCollection
import masecla.modrinth4j.model.team.ModrinthPermissionMask
import masecla.modrinth4j.model.version.ProjectVersion
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.tomlj.Toml
import stracciatella.curse.*
import java.time.Instant
import java.util.*
import javax.inject.Inject

abstract class ModListGenerator @Inject constructor(
    objects: ObjectFactory,
    private val fileSystem: FileSystemOperations
) : DefaultTask() {
    @Input
    val version = objects.property<String>()

    @Input
    val curseForgeMods = objects.setProperty<Int>()

    @Input
    val modrinthMods = objects.setProperty<String>()

    @OutputFile
    val outputFile: RegularFileProperty = objects.fileProperty()

    init {
        outputFile.convention { temporaryDir.resolve("mods.versions.toml") }
    }

    fun setup(path: Any) {
        val file = project.file(path)
        val toml = file.inputStream().use { Toml.parse(it) }

        if (toml.contains("version"))
            version.set(toml.getString("version"))

        if (toml.contains("curse")) {
            val array = toml.getArrayOrEmpty("curse")
            for (element in array.toList()) {
                if (element !is Long) throw IllegalArgumentException("Bad entry: ${element.javaClass} is not of type Long")
                curseForgeMods.add(element.toInt())
            }
        }

        if (toml.contains("modrinth")) {
            val array = toml.getArrayOrEmpty("modrinth")
            for (element in array.toList()) {
                if (element !is String) throw IllegalArgumentException("Bad entry: ${element.javaClass} is not of type String")
                modrinthMods.add(element)
            }
        }
    }

    @TaskAction
    fun run() {
        val agent = UserAgent.builder().authorUsername("DasBabyPixel").projectName("stracciatella").contact("dasbabypixel@gmail.com").build()
        val collectors = listOf(ModrinthCollector(agent), CurseCollector(agent))


        val registeredMods = HashSet<String>()
        val entries = ArrayList<MavenModEntry>()

        collectors.forEach {
            entries.addAll(it.collectMods(this, registeredMods))
        }

        entries.forEach {
            println(it)
        }

        val builder = StringBuilder()
        builder.appendLine("[libraries]")
        entries.forEach {
            builder.appendLine("${it.slug} = { group = \"${it.group}\", name = \"${it.name}\", version = \"${it.version}\" }")
        }

        builder.appendLine().appendLine("[bundles]")
        builder.appendLine("mods = [")
        entries.forEach {
            builder.appendLine("\"${it.slug}\",".prependIndent(" ".repeat(4)))
        }
        builder.appendLine("]")

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.bufferedWriter().use {
            it.write(builder.toString())
        }
    }
}

interface ModCollector {
    fun collectMods(
        task: ModListGenerator,
        registeredMods: MutableSet<String>
    ): List<MavenModEntry>
}

class CurseCollector(val agent: UserAgent) : ModCollector {
    override fun collectMods(
        task: ModListGenerator,
        registeredMods: MutableSet<String>
    ): List<MavenModEntry> {
        val curseAPI = CurseAPI.curseAPI(agent).join()

        val registered = HashMap<String, Mod>()
        val entries = ArrayList<MavenModEntry>()
        val missingMods = LinkedList(task.curseForgeMods.get())
        while (missingMods.isNotEmpty()) {
            val element = missingMods.removeLast()
            val mod = curseAPI.mods.getMod(element).join().data
            val slug = mod.slug
            if (registeredMods.contains(slug)) continue
            registeredMods.add(slug)
            registered[slug] = mod

            val response = curseAPI.files.getModFiles(mod.id, task.version.get(), ModLoaderType.Fabric).join()
            val files = response.data
            if (files.isEmpty()) {
                System.err.println("No version found for curse project ${mod.id}")
                continue
            }
            val file = files.first()

            entries.add(MavenModEntry("curse.maven", "${mod.slug}-${mod.id}", file.id.toString(), mod.slug))

            val dependencies = file.dependencies
            dependencies.forEach {
                if (it.relationType == FileRelationType.RequiredDependency) {
                    missingMods.add(it.modId)
                }
            }
        }
        return entries
    }
}

class ModrinthCollector(val agent: UserAgent) : ModCollector {
    override fun collectMods(
        task: ModListGenerator,
        registeredMods: MutableSet<String>
    ): List<MavenModEntry> {
        val api = ModrinthAPI.rateLimited(agent, "")

        val registered = HashMap<String, Project>()
        val entries = ArrayList<MavenModEntry>()
        val missingMods = LinkedList(task.modrinthMods.get())
        while (missingMods.isNotEmpty()) {
            val element = missingMods.removeLast()
            println("Project: $element")
            val mod = api.projects().get(element).join()
            val slug = mod.slug
            if (registeredMods.contains(slug)) continue
            registeredMods.add(slug)
            registered[slug] = mod

            val request = GetProjectVersionsRequest.builder().featured(true).loaders(listOf("fabric")).gameVersions(listOf(task.version.get())).build()
            val projectVersions = ArrayList(api.versions().getProjectVersions(slug, request).join())
            request.featured = false
            projectVersions.addAll(api.versions().getProjectVersions(slug, request).join())
            if (projectVersions.isEmpty()) {
                System.err.println("No version found for modrinth project ${mod.slug}. Trying next best version")
                request.gameVersions = listOf()
                projectVersions.addAll(api.versions().getProjectVersions(slug, request).join())
                projectVersions.add(api.versions().getVersion(mod.versions.last()).join())
            }
            val latestVersion = projectVersions.first()

            entries.add(MavenModEntry("maven.modrinth", mod.slug, latestVersion.id, mod.slug))

            val dependencies = latestVersion.dependencies
            dependencies.forEach {
                if (it.dependencyType == ProjectVersion.ProjectDependencyType.REQUIRED) {
                    missingMods.add(it.projectId)
                }
            }
        }
        return entries
    }
}

data class MavenModEntry(
    val group: String,
    val name: String,
    val version: String,
    val slug: String
)
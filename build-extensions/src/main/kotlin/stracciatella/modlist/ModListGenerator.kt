package stracciatella.modlist

import masecla.modrinth4j.client.agent.UserAgent
import masecla.modrinth4j.main.ModrinthAPI
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import stracciatella.curse.CurseAPI
import stracciatella.curse.ModLoaderType
import stracciatella.curse.ModsSearchSortField
import stracciatella.curse.SortOrder
import javax.inject.Inject

abstract class ModListGenerator @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @Input
    val version = objects.property<String>()

    @TaskAction
    fun run() {
        val agent = UserAgent.builder().authorUsername("DasBabyPixel").projectName("stracciatella").contact("dasbabypixel@gmail.com").build()
        val apiKey = ""
        val api = ModrinthAPI.rateLimited(agent, apiKey)
        api.projects()["fabric-api"].thenAccept {
            println(it.downloads)
        }.join()
        val curseAPI = CurseAPI.curseAPI(agent).join()
        val minecraft = curseAPI.games.getGames().join().data.first { it.name == "Minecraft" }
        val mods = curseAPI.categories.getCategories(gameId = minecraft.id).join().data.filter { it.isClass ?: false }.first { it.name == "Mods" }
//        1.20.4 10407 75125

        val res = curseAPI.mods.searchMods(gameId = minecraft.id, sortOrder = SortOrder.DESC, modLoaderType = ModLoaderType.Fabric, gameVersionTypeId = 75125, gameVersion = "1.20.4", sortField = ModsSearchSortField.Popularity).join()
        res.data.forEach {
            println(it.name)
        }


//        val gameId = 1
//        val response = curseAPI.mods.searchMods(CurseSearchMods.Request(gameId)).join()

//        val mods = CurseGetMods(curseHTTPClient, gson).sendRequest(CurseGetMods.Request(intArrayOf(579192))).join()
//        println(gson.toJson(mods))
//        mods.data.forEach {
//            println(it.name)
//        }
    }
}
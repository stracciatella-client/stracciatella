package stracciatella.modlist

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import masecla.modrinth4j.client.HttpClient
import masecla.modrinth4j.endpoints.generic.Endpoint
import masecla.modrinth4j.endpoints.generic.empty.EmptyRequest

class CustomGetProject(client: HttpClient, gson: Gson) : Endpoint<JsonObject, EmptyRequest>(client,gson) {

    override fun getEndpoint(): String {
        return "/project/{id}"
    }

    override fun getRequestClass(): TypeToken<EmptyRequest> {
        return TypeToken.get(EmptyRequest::class.java)
    }

    override fun getResponseClass(): TypeToken<JsonObject> {
        return TypeToken.get(JsonObject::class.java)
    }

    override fun requiresBody(): Boolean {
        return false
    }
}
package net.stracciatella.generator

import net.stracciatella.generator.Corrections.GET_VERSIONS_V2
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

const val CURSEFORGE_DOCS_URL = "https://docs.curseforge.com/rest-api/"
const val CURSEFORGE_API_URL = "https://api.curseforge.com" // NO SLASH AT END

private const val CURSEFORGE_CORE_API_PREFIX = "curseforge-core-api-"

fun main() {
    val connection = Jsoup.connect(CURSEFORGE_DOCS_URL)
    val document = connection.get()
    val endpoints: MutableMap<String, Endpoints> = HashMap()
    val entries = document.body().children().first()!!

    // Register basic endpoints
    entries.forEachNode {
        if (it !is Element) return@forEachNode
        if (it.id().isEmpty()) return@forEachNode
        if (!it.id().startsWith(CURSEFORGE_CORE_API_PREFIX)) return@forEachNode
        val endpointId = it.id().substring(CURSEFORGE_CORE_API_PREFIX.length)
        val endpointName = it.text()
        endpoints[endpointId] = Endpoints(endpointId, endpointName)
    }


    // Register schemas
    val schemasElement = entries.getElementById("schemas") ?: throw IllegalStateException("Schemas null")
    val schemaProvider = SchemaProvider(schemasElement)

    endpoints.entries.forEach {
        val element = entries.getElementById(CURSEFORGE_CORE_API_PREFIX + it.key)!!
        var entry: Element? = findNextEndpointElement(element)
        while (entry != null) {

            val endpoint = createEndpoint(entry, schemaProvider)
            it.value.endpoints.add(endpoint)

            entry = findNextEndpointElement(entry)
        }
    }

    generate(endpoints, schemaProvider)
}

private fun generate(
    endpointsMap: Map<String, Endpoints>,
    schemaProvider: SchemaProvider
) {
    Path("CurseAPISchema.kt").bufferedWriter().use { writer ->

        writer.write(
            """
            @file:Suppress("unused", "CanBeParameter")
            package stracciatella.curse
            
            import com.google.gson.Gson
            import com.google.gson.GsonBuilder
            import com.google.gson.JsonObject
            import com.google.gson.TypeAdapter
            import com.google.gson.TypeAdapterFactory
            import com.google.gson.reflect.TypeToken
            import com.google.gson.stream.JsonReader
            import com.google.gson.stream.JsonToken
            import com.google.gson.stream.JsonWriter
            import masecla.modrinth4j.client.HttpClient
            import masecla.modrinth4j.client.agent.UserAgent
            import masecla.modrinth4j.endpoints.generic.Endpoint
            import okhttp3.HttpUrl
            import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
            import okhttp3.MediaType.Companion.toMediaType
            import okhttp3.Request
            import okhttp3.RequestBody.Companion.toRequestBody
            import okhttp3.internal.http.HttpMethod
            import java.net.URI
            import java.util.concurrent.CompletableFuture
            import java.util.concurrent.ConcurrentHashMap
            import java.util.regex.Pattern
            import kotlin.reflect.full.companionObjectInstance
        """.trimIndent()
        )
        writer.newLine()
        writer.newLine()
        writer.write(curseAPIToString(endpointsMap))
        writer.newLine()
        writer.newLine()

        writer.write(endpointsMap.values.joinToString(transform = {
            endpointsToString(it)
        }, separator = "\n", postfix = "\n"))
        writer.write(endpointsMap.values.joinToString(transform = {
            endpointToString(it)
        }, separator = "\n\n", postfix = "\n\n"))

        writer.write(schemaProvider.schemaMap.values.map {
            schemaToString(it)
        }.filter { it.isNotEmpty() }.joinToString(separator = "\n\n", postfix = "\n\n"))

        writer.write(
            """
            abstract class CursedEnum(val id: Int) {
                companion object {
                    private val REGISTERED: MutableMap<Class<out Any>, MutableMap<Int, CursedEnum>> = ConcurrentHashMap()
                    fun registry(cls: Class<out Any>): MutableMap<Int, CursedEnum> {
                        return REGISTERED.computeIfAbsent(cls) { ConcurrentHashMap() }
                    }
                }

                init {
                    @Suppress("LeakingThis")
                    registry(this.javaClass)[id] = this
                }
            }
        """.trimIndent()
        )
        writer.newLine()
        writer.newLine()
        writer.write(
            """
            abstract class CurseEndpoint<O, I>(
                val client: CurseHTTPClient,
                gson: Gson,
                private val endpoint: String,
                private val method: String,
                private val jsonBody: Boolean,
                requestType: Class<I>,
                responseType: Class<O>
            ) : Endpoint<O, I>(client, gson) {
            
                private val requestType = TypeToken.get(requestType)
                private val responseType = TypeToken.get(responseType)
            
                fun sendRequest(
                    request: I,
                    pathParams: Map<String, String?>,
                    queryParams: Map<String, String?>
                ): CompletableFuture<O> {
                    val url = getReplacedUrl(request, pathParams.filterValues {it != null})
                    return client.connect(url, queryParams.filterValues {it != null}).thenApply { c ->
                        if (HttpMethod.permitsRequestBody(method))
                            c.method(method, "".toRequestBody("application/json; charset=utf-8".toMediaType()))
                        else
                            c.method(method, null)
            
                        if (requiresBody()) {
                            val jsonBody = this.gson.toJsonTree(request, requestClass.type)
                            if (isJsonBody) {
                                c.method(method, this.gson.toJson(jsonBody).toRequestBody("application/json; charset=utf-8".toMediaType()))
                            }
                        }
            
                        val response = executeRequest(c)
                        val body = response.body
            
                        checkBodyForErrors(body)
                    }
                }
            
                override fun getMethod(): String {
                    return method
                }
            
                override fun getEndpoint(): String {
                    return endpoint
                }
                
                override fun isJsonBody(): Boolean {
                    return jsonBody
                }
                
                override fun getRequestClass(): TypeToken<I> {
                    return requestType
                }
                
                override fun getResponseClass(): TypeToken<O> {
                    return responseType
                }
            }
            
            interface RawEnum {
                val data: String
            }

            class CurseHTTPClient(private val apiKey: String, private val baseUrl: String = CurseAPI.API, userAgent: UserAgent) : HttpClient(userAgent, baseUrl, apiKey) {
                override fun connect(url: String, queryParams: Map<String, String?>?): CompletableFuture<Request.Builder> {
                    return nextRequest().thenApply {
                        val parsedUrl: HttpUrl
                        if (!queryParams.isNullOrEmpty()) {
                            val builder: HttpUrl.Builder = (baseUrl + url).toHttpUrlOrNull()!!.newBuilder()
                            for ((key, value) in queryParams) {
                                if(value != null) {
                                    builder.addQueryParameter(key, value)
                                }
                            }
                            parsedUrl = builder.build()
                        } else {
                            parsedUrl = (baseUrl + url).toHttpUrlOrNull()!!
                        }

                        val connection: Request.Builder = Request.Builder().url(parsedUrl)
                        connection.header("x-api-key", apiKey)
                        connection
                    }
                }

                override fun nextRequest(): CompletableFuture<Void> {
                    return CompletableFuture.completedFuture(null)
                }
            }
        """.trimIndent()
        )
        writer.newLine()
    }
}

private fun curseAPIToString(map: Map<String, Endpoints>): String {
    return """
        class CurseAPI private constructor(agent: UserAgent) {
            companion object {
                const val API = "$CURSEFORGE_API_URL"
                fun curseAPI(agent: UserAgent): CompletableFuture<CurseAPI> {
                    return CompletableFuture.supplyAsync { CurseAPI(agent) }
                }
            }
            
            private val apiKey: String
            private val gson: Gson
            private val httpClient: CurseHTTPClient
            
            init {
                val url = URI.create("https://arch.b4k.co/vg/thread/388569358").toURL()
                val connection = url.openConnection()
                
                apiKey = connection.inputStream.use {
                    val text = it.bufferedReader().readText()
                    val pattern = Pattern.compile("settings and put ")
                    val matcher = pattern.matcher(text)
                    if (matcher.find()) {
                        val end = matcher.end()
                        val startApiKey = text.substring(end)
                        val extracted = startApiKey.substring(0, startApiKey.indexOf(' '))
                        return@use extracted
                    } else {
                        println("Not matched")
                    }
                    throw IllegalStateException()
                }
                httpClient = CurseHTTPClient(apiKey, userAgent = agent)

                gson = GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(object : TypeAdapterFactory {
                    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                        if (CursedEnum::class.java.isAssignableFrom(type.rawType)) {
                            type.rawType.kotlin.companionObjectInstance // initialize the companion object for the registry access
                            val registry = CursedEnum.registry(type.rawType)
                            return object : TypeAdapter<T>() {
                                override fun write(out: JsonWriter, value: T?) {
                                    if (value == null) {
                                        out.nullValue()
                                    } else {
                                        out.value((value as CursedEnum).id)
                                    }
                                }
        
                                override fun read(`in`: JsonReader): T? {
                                    if (`in`.peek() == JsonToken.NULL) {
                                        return null
                                    }
                                    @Suppress("UNCHECKED_CAST")
                                    return registry[`in`.nextInt()] as T
                                }
                            }
                        } else if (RawEnum::class.java.isAssignableFrom(type.rawType)) {
                            if (!type.rawType.isEnum) throw IllegalArgumentException("Classes implementing RawEnum must be enums")
                            val data = type.rawType.asSubclass(RawEnum::class.java).enumConstants
                            val registry = HashMap<String, RawEnum>()
                            for (entry in data)
                                registry[entry.data] = entry
                            return object : TypeAdapter<T>() {
                                override fun write(out: JsonWriter, value: T?) {
                                    if (value == null) {
                                        out.nullValue()
                                    } else {
                                        out.value((value as RawEnum).data)
                                    }
                                }
        
                                override fun read(`in`: JsonReader): T? {
                                    if (`in`.peek() == JsonToken.NULL) {
                                        return null
                                    }
                                    @Suppress("UNCHECKED_CAST")
                                    return registry[`in`.nextString()] as T
                                }
                            }
                        }
                        return null
                    }
                }).create()
            }
            
        {endpoints}
        
        }
        
        fun toQueryParam(
            gson: Gson,
            any: Any?
        ): String? {
            val json = gson.toJsonTree(any)
            if (json.isJsonPrimitive) {
                val primitive = json.asJsonPrimitive
                return if (primitive.isString) primitive.asString
                else if (primitive.isNumber) primitive.asNumber.toString()
                else primitive.asBoolean.toString()
            }
            throw IllegalArgumentException()
        }
    """.trimIndent().replace("{endpoints}", curseAPIEndpointsToString(map).prependIndent(" ".repeat(4)))
}

private fun curseAPIEndpointsToString(map: Map<String, Endpoints>): String {
    return map.entries.joinToString(transform = {
        "val ${it.key}: ${it.value.className()} = ${it.value.className()}(httpClient, gson)"
    }, separator = "\n")
}

private fun schemaToString(schema: Schema): String {
    return when (schema) {
        is ArraySchema -> ""
        is CompositeSchema -> schemaToString(schema)
        is EnumSchema -> schemaToString(schema)
        is OptionalSchema -> ""
        is PrimitiveSchema -> ""
        is RawEnumSchema -> schemaToString(schema)
    }
}

private fun schemaToString(schema: RawEnumSchema): String {
    val className = schema.className()
    return """
        enum class $className(override val data: String) : RawEnum {
        {entries};
        }
    """.trimIndent().replace("{entries}", schema.values.joinToString(transform = {
        "${it.uppercase()}(\"$it\")"
    }, separator = ",\n"))
}

private fun schemaToString(schema: EnumSchema): String {
    val className = schema.className()
    return """
        class $className(id: Int) : CursedEnum(id) {
            companion object {
        {enumValues}
            }
        }
    """.trimIndent().replace("{enumValues}", schemaToStringEnumValues(schema).prependIndent(" ".repeat(8)))
}

private fun schemaToStringEnumValues(schema: EnumSchema): String {
    val className = schema.className()
    return schema.values.entries.joinToString(transform = {
        "val ${it.value} = $className(${it.key})"
    }, separator = "\n")
}

private fun schemaToString(schema: CompositeSchema): String {
    val className = schema.className()
    return """
        class $className(
        {arguments}
        )
    """.trimIndent().replace("{arguments}", schemaArgumentsToString(schema).prependIndent(" ".repeat(4)))
}

private fun schemaArgumentsToString(schema: CompositeSchema): String {
    return schema.map.entries.joinToString(transform = {
        (if (it.value.description.isEmpty() || it.value.description == "none") "" else "/**\n * " + it.value.description + "\n*/\n") + "val ${it.key}: ${it.value.type.className()}"
    }, separator = ",\n")
}

private fun endpointToString(endpoints: Endpoints): String {
    return endpoints.endpoints.joinToString(transform = {
        endpointToString(it)
    }, separator = "\n\n")
}

private fun endpointToString(endpoint: Endpoint): String {
    val isJsonBody = endpoint.request.bodyParameters.isNotEmpty()
    return """
        class ${endpoint.className()}(
            client: CurseHTTPClient, 
            gson: Gson
        ) : CurseEndpoint<${endpoint.response.className()}, ${endpoint.className()}.Request>(
        client, gson, "${endpoint.url}", "${endpoint.method}", $isJsonBody, Request::class.java, ${endpoint.response.className()}::class.java) {
        {endpointRequest}
        }
    """.trimIndent().replace("{endpointRequest}", endpointRequestToString(endpoint).prependIndent(" ".repeat(4)))
}

private fun endpointRequestToString(endpoint: Endpoint): String {
    if (endpoint.request.bodyParameters.isEmpty()) return "class Request"
    return """
        class Request(
        {params}
        )
    """.trimIndent().replace("{params}", endpoint.request.bodyParameters.joinToString(transform = {
        "val ${it.name}: ${it.type.className() + if (it.required) "" else "? = null"}"
    }, separator = ",\n")).prependIndent(" ".repeat(4))
}

private fun endpointsToString(endpoints: Endpoints): String {
    return """
                class ${endpoints.className()}(private val client: CurseHTTPClient, private val gson: Gson) {
                {methods}
                }
                
            """.trimIndent().replace("{methods}", endpointsMethodsToString(endpoints))
}

private fun endpointsMethodsToString(endpoints: Endpoints): String {
    return endpoints.endpoints.joinToString(transform = {
        endpointsMethodToString(it)
    }, separator = "\n\n")
}

private fun endpointsMethodToString(endpoint: Endpoint): String {
    val endpointName = endpoint.name.classCorrections()
    val endpointClassName = endpoint.className()

    return """
        /**
         * ${endpoint.description}
         */
        fun ${endpointName[0].lowercase() + endpointName.substring(1)}(${endpointsMethodArgumentsToString(endpoint)}): CompletableFuture<${endpoint.response.className()}> {
            return $endpointClassName(client, gson).sendRequest(${endpointMethodArgumentsFill(endpoint)}, ${
        endpointsMethodsParametersToString(endpoint.request.pathParameters)
    }, ${
        endpointsMethodsParametersToString(endpoint.request.queryParameters)
    })
        }
    """.trimIndent().prependIndent(" ".repeat(4))
}

private fun endpointMethodArgumentsFill(endpoint: Endpoint): String {
    val includeRequest = endpoint.request.bodyParameters.isNotEmpty()
    return if (includeRequest) "request" else "${endpoint.className()}.Request()"
}

private fun endpointsMethodArgumentsToString(endpoint: Endpoint): String {
    val includeRequest = endpoint.request.bodyParameters.isNotEmpty()
    val arguments: List<String> = endpoint.request.pathParameters.plus(endpoint.request.queryParameters).map {
        "${it.name}: ${it.type.className()}${if (it.required) "" else "? = null"}"
    }
    val isFullyOptional = endpoint.request.bodyParameters.none { it.required }
    val finalArguments = if (includeRequest) listOf("request: ${endpoint.className()}.Request${if (isFullyOptional) " = ${endpoint.className()}.Request()" else ""}", *arguments.toTypedArray())
    else arguments

    return finalArguments.joinToString(transform = { it }, separator = ", ")
}

private fun endpointsMethodsParametersToString(params: List<RequestParameterEntry>): String {
    return "mapOf(${
        params.stream().map {
            "\"${it.name}\" to ${if(it.type .name == PrimitiveSchema.STRING) it.name else if(it.required) "toQueryParam(gson, ${it.name})" else "${it.name}?.let {toQueryParam(gson, it) }"}"
//            "\"${it.name}\" to ${it.name}${if (it.type.name == PrimitiveSchema.STRING) "" else "${if (it.required) "" else "?"}.let {toQueryParam(gson, it)}"}"
        }.toList().joinToString(", ")
    })"
}

private fun createEndpoint(
    entry: Element,
    schemaProvider: SchemaProvider
): Endpoint {
    val name = entry.text()
    val docsUrl = CURSEFORGE_DOCS_URL + "#" + entry.id()
    var method: String? = null
    var url: String? = null
    var description: String? = null

    var currentEntry: Element? = entry
    while (currentEntry != null) {
        if (currentEntry.tagName() == "p") {
            if (currentEntry.childrenSize() == 1) {
                val child = currentEntry.child(0)
                if (child.tagName() == "code") {
                    val split = child.text().split(" ", limit = 2)
                    method = split[0]
                    url = split[1]
                    description = currentEntry.nextElementSibling()?.text()
                    break
                }
            }
        }
        currentEntry = currentEntry.nextElementSibling()
    }
    if (method == null || url == null || description == null) throw IllegalStateException("Method/URL/Description not found for endpoint $name")

    val parametersElement = entry.parent()?.getElementById(entry.id().replace(GET_VERSIONS_V2) + "-parameters") ?: throw IllegalStateException("Parameters not found for $name")

    val pathParameters = ArrayList<RequestParameterEntry>()
    val queryParameters = ArrayList<RequestParameterEntry>()
    val bodyParameters = ArrayList<RequestParameterEntry>()
    parametersElement.nextElementSibling()?.child(1)?.children()?.forEach {
        val parameterName = it.child(0).text()
        val `in` = it.child(1).text()
        val type = schemaProvider.getSchema(it.child(2).text())
        val required = it.child(3).text().toBoolean()
        val parameterDescription = it.child(4).text()
        val parameterEntry = RequestParameterEntry(parameterName, type, required, parameterDescription)
        when (`in`) {
            "path" -> {
                pathParameters.add(parameterEntry)
            }

            "body" -> {
                bodyParameters.add(parameterEntry)
            }

            "query" -> {
                queryParameters.add(parameterEntry)
            }

            else -> {
                throw IllegalStateException("`in` not found: $`in` by $name")
            }
        }
    }
    val request = EndpointRequest(queryParameters, pathParameters, bodyParameters)

    val responsesElement = entry.parent()?.getElementById(entry.id().replace(GET_VERSIONS_V2) + "-responses") ?: throw IllegalStateException("Responses not found for $name")

    val schemaString = responsesElement.nextElementSibling()?.child(1)?.child(0)?.child(3)?.text() ?: throw IllegalStateException()

    val response = schemaProvider.getSchema(schemaString)

    return Endpoint(name, method, url, description, docsUrl, request, response)
}

enum class Corrections(val pair: Pair<String, String>) {
    GET_VERSIONS_V2("get-versions-v2" to "get-versions---v2"), ADDITIONAL_PROPERTIES("» additionalProperties" to "additionalProperties");
}

fun Schema.className(): String {
    return when (this) {
        is PrimitiveSchema -> primitive
        is CompositeSchema -> name.classCorrections()
        is RawEnumSchema -> name.classCorrections()
        is EnumSchema -> name.classCorrections()
        is ArraySchema -> "List<${target.className()}>"
        is OptionalSchema -> "${target.className()}?"
    }
}

fun Endpoint.className(): String {
    return "Curse${name.classCorrections()}"
}

fun Endpoints.className(): String {
    return "${name.classCorrections()}Endpoints"
}

fun String.classCorrections(): String {
    return replace(" ", "").replace("-", "")
}

fun String.replace(correction: Corrections): String {
    return replace(correction.pair.first, correction.pair.second)
}

class Endpoints(
    val id: String,
    val name: String,
    val endpoints: MutableList<Endpoint> = ArrayList()
)

class Endpoint(
    val name: String,
    val method: String,
    val url: String,
    val description: String,
    val docsUrl: String,
    val request: EndpointRequest,
    val response: Schema
)

class EndpointRequest(
    val queryParameters: List<RequestParameterEntry>,
    val pathParameters: List<RequestParameterEntry>,
    val bodyParameters: List<RequestParameterEntry>
)

class RequestParameterEntry(
    val name: String,
    val type: Schema,
    val required: Boolean,
    val description: String
)

class SchemaProvider(
    schemasElement: Element
) {
    private val schemas: MutableMap<String, Schema> = HashMap()
    val schemaMap: Map<String, Schema> = schemas
    private val possibleSchemasElements = schemasElement.nextElementSiblings()

    init {
        schemas[PrimitiveSchema.INT32] = PrimitiveSchema(PrimitiveSchema.INT32, "Int")
        schemas[PrimitiveSchema.INT64] = PrimitiveSchema(PrimitiveSchema.INT64, "Long")
        schemas[PrimitiveSchema.STRING] = PrimitiveSchema(PrimitiveSchema.STRING, "String")
        schemas[PrimitiveSchema.DATE_TIME] = PrimitiveSchema(PrimitiveSchema.DATE_TIME, "String")
        schemas[PrimitiveSchema.BOOLEAN] = PrimitiveSchema(PrimitiveSchema.BOOLEAN, "Boolean")
        schemas[PrimitiveSchema.NUMBER_DECIMAL] = PrimitiveSchema(PrimitiveSchema.NUMBER_DECIMAL, "Double")
        schemas[PrimitiveSchema.OBJECT] = PrimitiveSchema(PrimitiveSchema.OBJECT, "JsonObject")

        // fix SortOrder
        schemas["SortOrder"] = RawEnumSchema("SortOrder", listOf("asc", "desc"))
    }

    fun getSchema(name: String): Schema {
        if (name == "integer") return schemas[PrimitiveSchema.INT32]!!
        if (schemas.contains(name)) return schemas[name]!!
        val schema: Schema
        if (name.startsWith(ArraySchema.SCHEMA_BEGIN) && name.endsWith(ArraySchema.SCHEMA_END)) { // Array
            val enclosed = name.substring(ArraySchema.SCHEMA_BEGIN.length, name.length - ArraySchema.SCHEMA_END.length)
            val enclosedSchema = getSchema(enclosed)
            schema = ArraySchema(name, enclosedSchema)
        } else if (name.endsWith(OptionalSchema.SCHEMA_END)) {
            val enclosed = name.substring(0, name.length - OptionalSchema.SCHEMA_END.length)
            val enclosedSchema = getSchema(enclosed)
            schema = OptionalSchema(name, enclosedSchema)
        } else {
            val schemaElement = possibleSchemasElements.find { it.id() == "tocS_$name" }
            if (schemaElement == null) throw IllegalStateException("Schema $name could not be found!")
            val dataElement = schemaElement.getNextSchemaInformation()
            schema = if (dataElement.tagName() == "table") {
                // Composite
                createComposite(name, dataElement)
            } else if (dataElement.tagName() == "p") {
                // Enum
                createEnum(name, dataElement)
            } else {
                throw IllegalStateException("Unknown element: $dataElement}")
            }
        }
        schemas[name] = schema
        return schema
    }

    private fun createEnum(
        name: String,
        tableElement: Element
    ): EnumSchema {
        val map = HashMap<Int, String>()
        var element: Element? = tableElement
        do {
            val text = element!!.text()
            val split = text.split("=", limit = 2)
            map[split[0].toInt()] = split[1]
            element = element.nextElementSibling()
        } while (element != null && element.tagName() == "p")
        return EnumSchema(name, map)
    }

    private fun createComposite(
        name: String,
        tableElement: Element
    ): CompositeSchema {
        val tableBodyElement = tableElement.child(1)
        val map: MutableMap<String, SchemaEntry> = HashMap()
        for (tableBodyEntry in tableBodyElement.children()) {
            val schemaEntryName = tableBodyEntry.child(0).text().replace(Corrections.ADDITIONAL_PROPERTIES)
            val type = tableBodyEntry.child(1).text()
            val description = tableBodyEntry.child(2).text()

            val typeSchema = getSchema(type)
            val schemaEntry = SchemaEntry(typeSchema, description)
            map[schemaEntryName] = schemaEntry
        }
        return CompositeSchema(name, map)
    }
}

sealed class Schema(
    val name: String
)

private class RawEnumSchema(
    name: String,
    val values: List<String>
) : Schema(name)

private class OptionalSchema(
    name: String,
    val target: Schema
) : Schema(name) {
    companion object {
        const val SCHEMA_END = "¦null"
    }
}

private class EnumSchema(
    name: String,
    val values: Map<Int, String>
) : Schema(name)

private class CompositeSchema(
    name: String,
    val map: Map<String, SchemaEntry>
) : Schema(name)

private class ArraySchema(
    name: String,
    val target: Schema
) : Schema(name) {
    companion object {
        const val SCHEMA_BEGIN = "["
        const val SCHEMA_END = "]"
    }
}

private class PrimitiveSchema(
    name: String,
    val primitive: String
) : Schema(name) {
    companion object {
        const val INT32 = "integer(int32)"
        const val INT64 = "integer(int64)"
        const val STRING = "string"
        const val DATE_TIME = "string(date-time)"
        const val BOOLEAN = "boolean"
        const val NUMBER_DECIMAL = "number(decimal)"
        const val OBJECT = "object" // JsonObject
    }
}

class SchemaEntry(
    val type: Schema,
    val description: String
)

private fun findNextEndpointElement(startElement: Element): Element? {
    var element: Element? = startElement.nextElementSibling()
    while (element != null) {
        if (element.tagName() == "h2") return element
        if (element.tagName() == "h1") return null
        element = element.nextElementSibling()
    }
    return null
}

private fun Element.getNextSchemaInformation(): Element {
    var element: Element? = this
    do {
        element = element!!.nextElementSibling()
        if (element != null) {
            if (element.tagName() == "div" && element.className() == "highlight") {
                break
            }
        }
    } while (element != null)
    if (element == null) throw IllegalStateException()

    val nextSibling = element.nextElementSibling()!!
    if (nextSibling.tagName() == "h3") return nextSibling.nextElementSibling()!!
    if (nextSibling.tagName() == "p") return nextSibling.nextElementSibling()!!
    throw IllegalStateException("Bad Schema: ${this.id()}")
}
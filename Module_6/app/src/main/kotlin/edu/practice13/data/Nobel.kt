package edu.practice13.data

import edu.practice13.domain.Laureate
import edu.practice13.domain.NobelRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Nobel : NobelRepository {
    private val client = HttpClient(OkHttp) { expectSuccess = true }
    private var token = ""
    private var baseUrl = "http://10.0.2.2:8080"

    override suspend fun login(server: String, username: String, password: String) {
        require(server.startsWith("https://") || server.startsWith("http://"))
        baseUrl = server
        val response = client.post(endpoint("/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("username", username)
                put("password", password)
            }.toString())
        }
        val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        token = root.requiredText("token")
    }

    override suspend fun list(year: String, category: String): List<Laureate> {
        val response = client.get(endpoint("/prizes")) { bearerAuth(token) }
        return Json.parseToJsonElement(response.bodyAsText()).jsonArray.flatMap { value ->
            val prize = value.jsonObject
            val prizeYear = prize.requiredText("year")
            val prizeCategory = prize.requiredText("category")
            if ((year.isNotBlank() && year != prizeYear) || (category.isNotBlank() && category != prizeCategory)) {
                emptyList()
            } else {
                prize.getValue("laureates").jsonArray.map {
                    parseLaureate(it.jsonObject, prizeYear, prizeCategory)
                }
            }
        }
    }

    override suspend fun detail(item: Laureate): Laureate {
        val response = client.get(endpoint("/prizes/${item.year}/${item.category}")) { bearerAuth(token) }
        val prize = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val laureate = prize.getValue("laureates").jsonArray
            .first { it.jsonObject.requiredText("id") == item.id }.jsonObject

        return item.copy(
            motivation = laureate.requiredText("motivation"),
            country = laureate.optionalText("country"),
            portrait = laureate.optionalText("portraitUrl"),
        )
    }

    private fun parseLaureate(source: JsonObject, year: String, category: String) = Laureate(
        id = source.requiredText("id"),
        name = source.requiredText("fullName"),
        year = year,
        category = category,
        motivation = source.requiredText("motivation"),
        country = source.optionalText("country"),
        portrait = source.optionalText("portraitUrl"),
    )

    private fun JsonObject.requiredText(key: String) = getValue(key).jsonPrimitive.content

    private fun JsonObject.optionalText(key: String) = this[key]?.jsonPrimitive?.contentOrNull

    private fun endpoint(path: String) = baseUrl.trimEnd('/') + path

    override fun close() {
        client.close()
    }
}

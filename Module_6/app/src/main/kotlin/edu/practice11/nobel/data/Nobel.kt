package edu.practice11.nobel.data

import edu.practice11.nobel.domain.Laureate
import edu.practice11.nobel.domain.NobelRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull

fun JsonObject.en(key: String) = (this[key] as? JsonObject)?.text("en")

class Nobel : NobelRepository {
    private val client = HttpClient(OkHttp) { expectSuccess = true }

    override suspend fun list(year: String, category: String): List<Laureate> {
        val result = mutableListOf<Laureate>()
        val pageSize = 25
        var offset = 0

        while (true) {
            val response = client.get("https://api.nobelprize.org/2.1/nobelPrizes") {
                parameter("limit", pageSize)
                parameter("offset", offset)
                if (year.isNotBlank()) {
                    parameter("nobelPrizeYear", year)
                    parameter("yearTo", year)
                }
                if (category.isNotBlank()) parameter("nobelPrizeCategory", category)
            }
            val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val prizes = root["nobelPrizes"]?.jsonArray ?: error("Нет nobelPrizes")
            prizes.forEach { result.addAll(parseLaureates(it.jsonObject)) }

            offset += prizes.size
            val count = root["meta"]?.jsonObject?.get("count")?.jsonPrimitive?.intOrNull
            if (prizes.size < pageSize || (count != null && offset >= count)) break
        }
        return result
    }

    private fun parseLaureates(prize: JsonObject): List<Laureate> {
        val laureates = prize["laureates"]?.jsonArray ?: return emptyList()
        return laureates.map { value ->
            val laureate = value.jsonObject
            Laureate(
                id = laureate.text("id") ?: error("Нет id"),
                name = laureate.en("fullName") ?: laureate.en("orgName") ?: laureate.en("knownName")
                    ?: "Имя отсутствует",
                year = prize.text("awardYear").orEmpty(),
                category = prize.en("category").orEmpty(),
                motivation = laureate.en("motivation").orEmpty(),
            )
        }
    }

    override suspend fun detail(item: Laureate): Laureate {
        val response = client.get("https://api.nobelprize.org/2.1/laureate/${item.id}")
        val root = Json.parseToJsonElement(response.bodyAsText())
        val laureate = if (root is JsonArray) root.first().jsonObject else root.jsonObject
        val birthPlace = laureate["birth"]?.jsonObject?.get("place")?.jsonObject
        return item.copy(country = birthPlace?.en("country"), portrait = laureate.text("portraitUrl"))
    }

    override fun close() {
        client.close()
    }
}

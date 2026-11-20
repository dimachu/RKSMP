package edu.nobel

import edu.nobel.domain.NobelPrize
import edu.nobel.domain.PrizeRepository
import edu.nobel.domain.User
import edu.nobel.presentation.module
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoritesRoutesTest {
    @Test
    fun authenticatedFavoritesAreScopedToTheTokenOwner() = testApplication {
        application { module(TestRepository(), "test-only-key-long-enough-for-signing") }

        suspend fun login(name: String): String {
            val response = client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$name","password":"test-password"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            return Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("token").jsonPrimitive.content
        }

        val first = login("first")
        val second = login("second")
        val collection = "/users/me/prizes"
        val item = "$collection/1903-phy"
        assertEquals(HttpStatusCode.Unauthorized, client.post(item).status)
        assertEquals(HttpStatusCode.NotFound, client.post("$collection/missing") { bearerAuth(first) }.status)
        repeat(2) {
            assertEquals(HttpStatusCode.NoContent, client.post(item) { bearerAuth(first) }.status)
        }
        assertEquals(HttpStatusCode.NoContent, client.delete(item) { bearerAuth(second) }.status)
        assertEquals("[]", client.get(collection) { bearerAuth(second) }.bodyAsText())
        val favorites = Json.parseToJsonElement(client.get(collection) { bearerAuth(first) }.bodyAsText()).jsonArray
        assertEquals(listOf("1903-phy"), favorites.map { it.jsonObject.getValue("id").jsonPrimitive.content })
        assertEquals(HttpStatusCode.NoContent, client.delete(item) { bearerAuth(first) }.status)
        assertEquals("[]", client.get(collection) { bearerAuth(first) }.bodyAsText())
        val doc = Json.parseToJsonElement(client.get("/openapi.json").bodyAsText()).jsonObject
        assertTrue(doc.getValue("paths").jsonObject.containsKey("$collection/{prizeId}"))
    }
}

private class TestRepository : PrizeRepository {
    private val prize = NobelPrize("1903-phy", 1903, "phy", "Physics", emptyList())
    private val users = listOf(User(1, "first"), User(2, "second"))
    private val favorites = mutableSetOf<Pair<Int, String>>()

    override suspend fun all() = listOf(prize)
    override suspend fun user(id: Int) = users.find { it.id == id }
    override suspend fun login(username: String, password: String) =
        users.find { it.username == username && password == "test-password" }

    override suspend fun favorites(user: Int) = all().filter { user to it.id in favorites }
    override suspend fun add(user: Int, prize: String): Boolean {
        if (prize != this.prize.id) return false
        favorites.add(user to prize)
        return true
    }

    override suspend fun remove(user: Int, prize: String) {
        favorites.remove(user to prize)
    }
}

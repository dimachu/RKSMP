package edu.nobel

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import edu.nobel.data.LocalRepository
import edu.nobel.presentation.module
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiTest {
    private val secret = "test-only-signing-key-32-characters-long"

    @Test
    fun authenticationAndPrizeContract() = testApplication {
        application { module(LocalRepository(), secret) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/prizes").status)

        val rejected = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"learner","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, rejected.status)

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"learner","password":"practice"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val token = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("token").jsonPrimitive.content
        val decoded = JWT.decode(token)
        assertEquals(1800, (decoded.expiresAt.time - decoded.issuedAt.time) / 1000)

        val prizes = client.get("/prizes") { bearerAuth(token) }
        assertEquals(5, Json.parseToJsonElement(prizes.bodyAsText()).jsonArray.size)

        val winners = client.get("/prizes/1903/phy/laureates") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, winners.status)
        val winnerIds = Json.parseToJsonElement(winners.bodyAsText()).jsonArray.map {
            it.jsonObject.getValue("id").jsonPrimitive.content
        }
        assertEquals(listOf("4", "5", "6"), winnerIds)
        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/prizes/invalid/phy") { bearerAuth(token) }.status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/prizes/1904/phy") { bearerAuth(token) }.status,
        )

        val expired = JWT.create()
            .withIssuer("practice-nobel")
            .withAudience("practice-client")
            .withSubject("1")
            .withExpiresAt(Instant.now().minusSeconds(60))
            .sign(Algorithm.HMAC256(secret))
        for (path in listOf("/prizes", "/prizes/1903/phy", "/prizes/1903/phy/laureates")) {
            assertEquals(HttpStatusCode.Unauthorized, client.get(path).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(path) { bearerAuth(token + "x") }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(path) { bearerAuth(expired) }.status)
        }
    }
}

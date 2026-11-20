package edu.nobel.presentation

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import edu.nobel.domain.GetPrize
import edu.nobel.domain.GetPrizes
import edu.nobel.domain.PrizeRepository
import edu.nobel.plugins.plugins
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.Instant
import java.time.Year
import kotlinx.serialization.Serializable

@Serializable
data class Login(val username: String, val password: String)

@Serializable
data class Token(val token: String)

private val categories = setOf("phy", "che", "med", "lit", "pea", "eco")

fun Application.module(repository: PrizeRepository, secret: String) {
    plugins(repository, secret)
    val getPrizes = GetPrizes(repository)
    val getPrize = GetPrize(repository)

    routing {
        post("/auth/login") {
            val input = call.receive<Login>()
            val user = repository.login(input.username, input.password)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Неверный логин или пароль"))
            } else {
                call.respond(Token(createToken(user.id, secret)))
            }
        }

        authenticate("jwt") {
            get("/prizes") { call.respond(getPrizes()) }
            get("/prizes/{year}/{category}") { respondWithPrize(call, getPrize, laureatesOnly = false) }
            get("/prizes/{year}/{category}/laureates") { respondWithPrize(call, getPrize, laureatesOnly = true) }
        }
    }
}

private fun createToken(userId: Int, secret: String): String {
    val now = Instant.now()
    return JWT.create()
        .withIssuer("practice-nobel")
        .withAudience("practice-client")
        .withSubject(userId.toString())
        .withIssuedAt(now)
        .withExpiresAt(now.plusSeconds(1800))
        .sign(Algorithm.HMAC256(secret))
}

private suspend fun respondWithPrize(call: ApplicationCall, getPrize: GetPrize, laureatesOnly: Boolean) {
    val year = call.parameters["year"]?.toIntOrNull()
    val category = call.parameters["category"].orEmpty()
    if (year == null || year !in 1901..Year.now().value || category !in categories) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Неверный год или категория"))
        return
    }

    val prize = getPrize(year, category)
    if (prize == null) {
        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Премия не найдена"))
    } else if (laureatesOnly) {
        call.respond(prize.laureates)
    } else {
        call.respond(prize)
    }
}

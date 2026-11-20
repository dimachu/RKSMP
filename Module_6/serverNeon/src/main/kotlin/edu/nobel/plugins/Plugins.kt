package edu.nobel.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import edu.nobel.domain.PrizeRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException

fun Application.plugins(repository: PrizeRepository, secret: String) {
    require(secret.length >= 32)
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            if (cause is IllegalArgumentException || cause is BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Некорректный запрос"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Ошибка сервера"))
            }
        }
    }
    install(Authentication) {
        jwt("jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("practice-nobel")
                    .withAudience("practice-client")
                    .build(),
            )
            validate { credential ->
                val id = credential.payload.subject?.toIntOrNull()
                if (id != null && repository.user(id) != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Нужен действующий JWT"))
            }
        }
    }
}

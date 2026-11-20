package edu.nobel

import edu.nobel.data.LocalRepository
import edu.nobel.presentation.module
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val secret = System.getenv("JWT_SECRET") ?: error("Задайте JWT_SECRET длиной минимум 32 символа")
    require(secret.length >= 32)

    val repository = LocalRepository()
    val port = (System.getenv("PORT") ?: "8081").toInt()
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module(repository, secret)
    }.start(wait = true)
}

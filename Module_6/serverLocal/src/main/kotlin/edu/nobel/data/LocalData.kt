package edu.nobel.data

import edu.nobel.domain.NobelPrize
import edu.nobel.domain.PrizeRepository
import edu.nobel.domain.User
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt

fun seed(): List<NobelPrize> {
    val resource = checkNotNull(object {}.javaClass.getResourceAsStream("/prizes.json"))
    return resource.bufferedReader().use { Json.decodeFromString(it.readText()) }
}

class LocalRepository : PrizeRepository {
    private val prizes = seed()
    private val passwordHash = BCrypt.hashpw(System.getenv("DEMO_PASSWORD") ?: "practice", BCrypt.gensalt())

    override suspend fun all() = prizes

    override suspend fun login(username: String, password: String): User? =
        if (username == "learner" && BCrypt.checkpw(password, passwordHash)) {
            User(1, username)
        } else {
            null
        }

    override suspend fun user(id: Int) = if (id == 1) User(1, "learner") else null
}

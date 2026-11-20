package edu.nobel.domain

import kotlinx.serialization.Serializable

@Serializable
data class Laureate(
    val id: String,
    val fullName: String,
    val motivation: String,
    val portion: String,
    val country: String? = null,
    val portraitUrl: String? = null,
)

@Serializable
data class NobelPrize(
    val id: String,
    val year: Int,
    val category: String,
    val fullName: String,
    val laureates: List<Laureate>,
)

@Serializable
data class User(val id: Int, val username: String)

interface PrizeRepository {
    suspend fun all(): List<NobelPrize>
    suspend fun login(username: String, password: String): User?
    suspend fun user(id: Int): User?
    suspend fun favorites(user: Int): List<NobelPrize>
    suspend fun add(user: Int, prize: String): Boolean
    suspend fun remove(user: Int, prize: String)
}

class GetPrizes(private val repository: PrizeRepository) {
    suspend operator fun invoke() = repository.all()
}

class GetPrize(private val repository: PrizeRepository) {
    suspend operator fun invoke(year: Int, category: String) =
        repository.all().find { it.year == year && it.category == category }
}

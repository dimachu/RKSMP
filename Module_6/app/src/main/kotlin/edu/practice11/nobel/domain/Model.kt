package edu.practice11.nobel.domain

import java.time.Year

data class Laureate(
    val id: String,
    val name: String,
    val year: String,
    val category: String,
    val motivation: String,
    val country: String? = null,
    val portrait: String? = null,
)

interface NobelRepository {
    suspend fun list(year: String, category: String): List<Laureate>
    suspend fun detail(item: Laureate): Laureate
    fun close()
}

class GetLaureates(private val repository: NobelRepository) {
    suspend operator fun invoke(year: String, category: String): List<Laureate> {
        if (year.isNotBlank()) {
            require(year.toIntOrNull() in 1901..Year.now().value) { "Год от 1901 до текущего" }
        }
        return repository.list(year, category)
    }
}

package edu.practice11.auth.domain

data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val image: String,
)

interface UsersRepository {
    suspend fun login(username: String, password: String): String
    suspend fun users(token: String): List<User>
    suspend fun user(id: Int, token: String): User
    fun close()
}

interface TokenStore {
    suspend fun read(): String?
    suspend fun save(token: String)
    suspend fun clear()
}

class GetUsers(private val repository: UsersRepository) {
    suspend operator fun invoke(token: String) = repository.users(token)
}

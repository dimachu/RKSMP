package edu.practice11.auth.data

import edu.practice11.auth.domain.User
import edu.practice11.auth.domain.UsersRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Users : UsersRepository {
    private val client = HttpClient(OkHttp) { expectSuccess = true }

    override suspend fun login(username: String, password: String): String {
        val response = client.post("https://dummyjson.com/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("username", username)
                put("password", password)
            }.toString())
        }
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("accessToken").jsonPrimitive.content
    }

    override suspend fun users(token: String): List<User> {
        val response = client.get("https://dummyjson.com/users") {
            parameter("limit", 0)
            bearerAuth(token)
        }
        val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return root.getValue("users").jsonArray.map { parseUser(it.jsonObject) }
    }

    override suspend fun user(id: Int, token: String): User {
        val response = client.get("https://dummyjson.com/users/$id") { bearerAuth(token) }
        return parseUser(Json.parseToJsonElement(response.bodyAsText()).jsonObject)
    }

    private fun parseUser(data: JsonObject) = User(
        id = data.getValue("id").jsonPrimitive.int,
        firstName = data.getValue("firstName").jsonPrimitive.content,
        lastName = data.getValue("lastName").jsonPrimitive.content,
        username = data.getValue("username").jsonPrimitive.content,
        email = data.getValue("email").jsonPrimitive.content,
        image = data.getValue("image").jsonPrimitive.content,
    )

    override fun close() {
        client.close()
    }
}

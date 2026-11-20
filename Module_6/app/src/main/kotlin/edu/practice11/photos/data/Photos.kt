package edu.practice11.photos.data

import com.google.gson.annotations.SerializedName
import edu.practice11.photos.domain.Photo
import edu.practice11.photos.domain.PhotoRepository
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class PhotoDto(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    @SerializedName("download_url") val download: String,
)

interface Picsum {
    @GET("v2/list")
    suspend fun list(@Query("page") page: Int, @Query("limit") limit: Int = 30): List<PhotoDto>
}

class Photos : PhotoRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://picsum.photos/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Picsum::class.java)

    override suspend fun list() = fetchPage().shuffled().map { photo ->
        Photo(
            photo.id, photo.author, photo.width, photo.height, photo.url, photo.download,
            "https://picsum.photos/id/${photo.id}/400/300",
        )
    }

    private suspend fun fetchPage(): List<PhotoDto> {
        return try {
            api.list(Random.nextInt(1, 11))
        } catch (failure: HttpException) {
            val status = failure.code()
            val reason = if (status == 403) failure.response()?.errorBody()?.byteStream().readReason() else null
            throw IOException(picsumHttpMessage(status, reason), failure)
        }
    }

    override suspend fun download(url: String, destination: OutputStream) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                val reason = if (status == 403) connection.errorStream.readReason() else null
                throw IOException(picsumHttpMessage(status, reason))
            }
            check(connection.contentType?.startsWith("image/") == true) { "Сервер вернул ответ без изображения" }
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    ensureActive()
                    val count = input.read(buffer)
                    if (count == -1) break
                    destination.write(buffer, 0, count)
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun picsumHttpMessage(status: Int, reason: String?): String = when {
    status == 403 && reason?.trim().equals("Geoblocked", ignoreCase = true) ->
        "Ограничение доступа Picsum для текущей сети (HTTP 403). Попробуйте другую сеть."
    status == 403 -> "Picsum отклонил запрос (HTTP 403)"
    else -> "Picsum вернул HTTP $status"
}

private fun InputStream?.readReason(): String? = try {
    this?.use { stream ->
        val bytes = ByteArray(64)
        var length = 0
        while (length < bytes.size) {
            val count = stream.read(bytes, length, bytes.size - length)
            if (count <= 0) break
            length += count
        }
        String(bytes, 0, length, Charsets.UTF_8)
    }
} catch (_: IOException) {
    null
}

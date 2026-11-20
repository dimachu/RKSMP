package edu.practice11.photos.domain

import java.io.OutputStream

data class Photo(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    val downloadUrl: String,
    val thumbnailUrl: String,
)

interface PhotoRepository {
    suspend fun list(): List<Photo>
    suspend fun download(url: String, destination: OutputStream)
}

class GetPhotos(private val repository: PhotoRepository) {
    suspend operator fun invoke() = repository.list()
}

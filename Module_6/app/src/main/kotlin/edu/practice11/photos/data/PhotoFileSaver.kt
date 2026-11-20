package edu.practice11.photos.data

import android.content.Context
import android.net.Uri
import edu.practice11.photos.domain.Photo
import edu.practice11.photos.domain.PhotoRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoFileSaver(context: Context, private val repository: PhotoRepository) {
    private val context = context.applicationContext

    suspend fun save(photo: Photo, uri: Uri) = withContext(Dispatchers.IO) {
        val temporary = File.createTempFile("photo", ".jpg", context.cacheDir)
        try {
            temporary.outputStream().use { repository.download(photo.downloadUrl, it) }
            check(temporary.length() > 0) { "Пустое изображение" }
            val output = checkNotNull(context.contentResolver.openOutputStream(uri, "wt"))
            output.use { destination ->
                temporary.inputStream().use { it.copyTo(destination) }
            }
        } finally {
            temporary.delete()
        }
    }
}

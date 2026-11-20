package edu.practice11.photos.presentation

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.practice11.photos.domain.PhotoRepository
import edu.practice11.photos.domain.GetPhotos
import edu.practice11.photos.domain.Photo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScreenModel(
    repository: PhotoRepository,
    private val savePhoto: suspend (Photo, Uri) -> Unit,
) : ViewModel() {
    private val getPhotos = GetPhotos(repository)
    private var request: Job? = null
    private var lastAction: (suspend () -> Unit)? = null

    var busy by mutableStateOf(false)
    var error by mutableStateOf("")
    var photos by mutableStateOf<List<Photo>>(emptyList())
    var selected by mutableStateOf<Photo?>(null)
    var message by mutableStateOf("")

    init {
        load()
    }

    fun load() = work {
        photos = getPhotos()
    }

    fun save(uri: Uri) {
        val photo = selected ?: return
        work {
            savePhoto(photo, uri)
            message = "Фото сохранено"
        }
    }

    fun retry() {
        lastAction?.let(::work)
    }

    private fun work(action: suspend () -> Unit) {
        lastAction = action
        request?.cancel()
        request = viewModelScope.launch {
            busy = true
            error = ""
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Ошибка сети"
            } finally {
                busy = false
            }
        }
    }
}

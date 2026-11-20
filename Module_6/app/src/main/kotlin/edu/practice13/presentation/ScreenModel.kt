package edu.practice13.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.practice13.domain.NobelRepository
import edu.practice13.domain.GetLaureates
import edu.practice13.domain.Laureate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScreenModel(private val repository: NobelRepository) : ViewModel() {
    private val getItems = GetLaureates(repository)
    private var request: Job? = null
    private var lastAction: (suspend () -> Unit)? = null

    var busy by mutableStateOf(false)
    var error by mutableStateOf("")
    var year by mutableStateOf("")
    var category by mutableStateOf("")
    var laureates by mutableStateOf<List<Laureate>>(emptyList())
    var selected by mutableStateOf<Laureate?>(null)

    var authenticated by mutableStateOf(false)
    var server by mutableStateOf("http://10.0.2.2:8080")
    var username by mutableStateOf("learner")
    var password by mutableStateOf("")

    fun login() = work {
        repository.login(server, username, password)
        password = ""
        authenticated = true
        lastAction = { laureates = getItems(year, category) }
        laureates = getItems(year, category)
    }

    fun load() = work {
        laureates = getItems(year, category)
    }

    fun detail(item: Laureate) = work {
        selected = repository.detail(item)
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

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}

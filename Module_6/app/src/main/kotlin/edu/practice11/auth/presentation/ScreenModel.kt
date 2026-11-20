package edu.practice11.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.practice11.auth.domain.UsersRepository
import edu.practice11.auth.domain.TokenStore
import edu.practice11.auth.domain.GetUsers
import edu.practice11.auth.domain.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScreenModel(private val repository: UsersRepository, private val tokens: TokenStore) : ViewModel() {
    private val getUsers = GetUsers(repository)
    private var request: Job? = null
    private var lastAction: (suspend () -> Unit)? = null

    var busy by mutableStateOf(false)
    var error by mutableStateOf("")
    var token by mutableStateOf<String?>(null)
    var initialized by mutableStateOf(false)
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var users by mutableStateOf<List<User>>(emptyList())
    var selected by mutableStateOf<User?>(null)

    init {
        work {
            val storedToken = tokens.read()
            token = storedToken
            initialized = true
            if (storedToken != null) {
                users = getUsers(storedToken)
            }
        }
    }

    fun login() = work {
        val accessToken = repository.login(username, password)
        tokens.save(accessToken)
        token = accessToken
        password = ""
        lastAction = { users = getUsers(accessToken) }
        users = getUsers(accessToken)
    }

    fun load() = work {
        users = getUsers(checkNotNull(token))
    }

    fun detail(user: User) = work {
        selected = repository.user(user.id, checkNotNull(token))
    }

    fun logout() = work {
        tokens.clear()
        token = null
        users = emptyList()
        selected = null
        password = ""
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

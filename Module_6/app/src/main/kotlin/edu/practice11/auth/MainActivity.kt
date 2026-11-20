package edu.practice11.auth

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.practice11.auth.data.Users
import edu.practice11.auth.data.TokenPreferences
import coil.compose.AsyncImage
import edu.practice11.auth.domain.User
import edu.practice11.auth.presentation.ScreenModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class MainActivity : ComponentActivity() {
    private val model by viewModels<ScreenModel> {
        viewModelFactory { initializer { ScreenModel(Users(), TokenPreferences(application)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 6", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    AuthScreen(model)
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(model: ScreenModel) {
    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val selected = model.selected
        when {
            model.initialized && model.token == null -> LoginForm(model)
            model.token != null -> {
                val listState = rememberLazyListState()
                if (selected != null) {
                    UserDetails(
                        user = selected,
                        model = model,
                        onBack = { model.selected = null },
                        onLogout = model::logout,
                    )
                } else {
                    UserList(model, listState)
                }
            }
            else -> Column(
                modifier = Modifier.widthIn(max = 440.dp).padding(20.dp).align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RequestStatus(model)
            }
        }
    }
}

@Composable
private fun LoginForm(model: ScreenModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        Box(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = availableHeight).padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Вход",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = model.username,
                    onValueChange = { model.username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Логин") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = model.password,
                    onValueChange = { model.password = it },
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Button(
                    enabled = !model.busy,
                    onClick = model::login,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text("Войти") }
                RequestStatus(model)
            }
        }
    }
}

@Composable
private fun RequestStatus(model: ScreenModel) {
    if (model.busy) {
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp))
        }
    }
    if (model.error.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(requestErrorMessage(model.error), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = model::retry) { Text("Повторить") }
            }
        }
    }
}

private val invalidLogin = Regex("""^Client request\(POST https?://[^/\s]+/auth/login\) invalid: (?:400|401)\b""")

private fun requestErrorMessage(error: String): String {
    if (invalidLogin.containsMatchIn(error)) return "Неверные логин или пароль"
    if (!error.startsWith("Client request(") && !error.startsWith("Server error(")) return error

    val bodyMarker = ". Text: \""
    val body = error.substringAfter(bodyMarker, "").removeSuffix("\"")
    val response = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
    val message = (response?.get("message") as? JsonPrimitive)?.contentOrNull
    if (!message.isNullOrBlank()) return message

    val status = error.substringBefore(bodyMarker).substringAfterLast(": ", "")
        .substringBefore(' ').toIntOrNull()
    return if (status == null) "Ошибка запроса" else "Ошибка запроса (HTTP $status)"
}

@Composable
private fun UserDetails(user: User, model: ScreenModel, onBack: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text("Профиль", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(user, "Аватар", Modifier.size(112.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            OutlinedButton(onClick = onLogout) { Text("Выйти") }
            RequestStatus(model)
        }
    }
}

@Composable
private fun UserList(model: ScreenModel, state: LazyListState) {
    Column(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 16.dp),
    ) {
        Text(
            "Пользователи",
            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        RequestStatus(model)
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(model.users, key = { it.id }) { user ->
                Surface(
                    onClick = { model.detail(user) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(user, null, Modifier.size(44.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                "${user.firstName} ${user.lastName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                user.username,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(user: User, description: String?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        AsyncImage(user.image, description, modifier = Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
    }
}

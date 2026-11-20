package edu.practice1

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) { PracticeScreens(intent.getIntExtra("edu.moduleui.INITIAL_TAB", 0) == 1) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeScreens(initialFeed: Boolean) {
    var showFeed by rememberSaveable { mutableStateOf(initialFeed) }
    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
        ) {
            PrimaryTabRow(selectedTabIndex = if (showFeed) 1 else 0) {
                Tab(selected = !showFeed, onClick = { showFeed = false }, text = { Text("Поиск") })
                Tab(selected = showFeed, onClick = { showFeed = true }, text = { Text("Лента") })
            }
            if (showFeed) SocialFeedScreen() else RepositorySearchScreen()
        }
    }
}

@Composable
private fun RepositorySearchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf(emptyList<String>()) }
    var searchError by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        singleLine = true,
        onValueChange = { updatedQuery ->
            query = updatedQuery
            val request = ++generation
            searchJob?.cancel()
            loading = true
            searchJob = scope.launch {
                try {
                    delay(500)
                    val search = async { searchRepositories(context, updatedQuery) }
                    val matches = search.await()
                    if (request == generation) {
                        rows = matches
                        searchError = ""
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (request == generation) searchError = failure.message ?: "Ошибка"
                } finally {
                    if (request == generation) loading = false
                }
            }
        },
        label = { Text("Репозиторий") },
    )
    if (loading) CircularProgressIndicator(Modifier.padding(vertical = 6.dp).size(20.dp))
    if (searchError.isNotBlank()) Text(searchError, color = MaterialTheme.colorScheme.error)
    if (rows.isEmpty() && query.isNotBlank() && !loading && searchError.isBlank()) {
        Text("Репозитории не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(rows) { row ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(row.substringBefore('\n'), style = MaterialTheme.typography.titleMedium)
                Text(
                    row.substringAfter('\n'),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private suspend fun searchRepositories(context: Context, query: String): List<String> =
    withContext(Dispatchers.IO) {
        readAssetArray(context, "github_repos.json").objects()
            .filter { it.getString("full_name").contains(query, ignoreCase = true) }
            .map(::formatRepository)
    }

private fun formatRepository(repository: JSONObject): String {
    val name = repository.getString("full_name")
    val description = repository.getString("description")
    val stars = repository.getInt("stargazers_count")
    val language = repository.getString("language")
    return "$name\n$description · ★$stars · $language"
}

@Composable
private fun SocialFeedScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cards = remember { mutableStateMapOf<Int, String>() }
    var refreshJob by remember { mutableStateOf<Job?>(null) }

    fun refresh() {
        refreshJob?.cancel()
        cards.clear()
        refreshJob = scope.launch {
            try {
                loadFeed(context) { id, text -> cards[id] = text }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                currentCoroutineContext().ensureActive()
                cards[-1] = "Ошибка: ${failure.message}"
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(Unit) { onDispose { refreshJob?.cancel() } }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Публикации", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = ::refresh) { Text("Обновить") }
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards.keys.sorted(), key = { it }) { id ->
            val card = cards[id].orEmpty()
            if (id == -1) {
                Text(card, color = MaterialTheme.colorScheme.error)
            } else {
                PostCard(card)
            }
        }
    }
}

@Composable
private fun PostCard(card: String) {
    val lines = card.split('\n', limit = 3)
    val header = lines.first()
    val status = header.substringBefore(' ')
    val title = header.substringAfter(' ').removePrefix("· ")
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                status,
                style = MaterialTheme.typography.labelMedium,
                color = if (status == "Error") colors.error else colors.primary,
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            lines.getOrNull(1)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            lines.getOrNull(2)?.takeIf(String::isNotBlank)?.let { comments ->
                HorizontalDivider(color = colors.outlineVariant)
                Text("Комментарии", style = MaterialTheme.typography.labelLarge)
                Text(comments, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

private data class Post(val id: Int, val title: String, val body: String)

private suspend fun loadFeed(context: Context, publishCard: (Int, String) -> Unit) {
    val (posts, comments) = withContext(Dispatchers.IO) {
        val posts = readAssetArray(context, "social_posts.json").objects().map {
            Post(it.getInt("id"), it.getString("title"), it.getString("body"))
        }
        val comments = readAssetArray(context, "comments.json").objects()
        posts to comments
    }
    supervisorScope {
        posts.forEach { post ->
            publishCard(post.id, "Loading · ${post.title}")
            launch {
                val card = loadPostCard(post, comments)
                currentCoroutineContext().ensureActive()
                publishCard(post.id, card)
            }
        }
    }
}

private suspend fun loadPostCard(post: Post, comments: List<JSONObject>): String = coroutineScope {
    val avatar = async { loadPart(200L + post.id * 110) { "🔵" } }
    val replies = async {
        loadPart(400L + (11 - post.id) * 90) {
            comments.filter { it.getInt("postId") == post.id }.joinToString { comment ->
                "${comment.getString("name")}: ${comment.getString("body")}"
            }
        }
    }
    val avatarText = avatar.await()
    val repliesText = replies.await()
    val state = if (avatarText == PART_ERROR || repliesText == PART_ERROR) "Error" else "Ready"
    "$state $avatarText ${post.title}\n${post.body}\n$repliesText"
}

private const val PART_ERROR = "⚠ placeholder"

private suspend fun loadPart(delayMs: Long, content: () -> String): String = try {
    delay(delayMs)
    if (Random.nextInt(15) == 0) error("Ошибка загрузки")
    content()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    PART_ERROR
}

private fun readAssetArray(context: Context, name: String): JSONArray =
    context.assets.open(name).bufferedReader().use { JSONArray(it.readText()) }

private fun JSONArray.objects(): List<JSONObject> = List(length(), ::getJSONObject)

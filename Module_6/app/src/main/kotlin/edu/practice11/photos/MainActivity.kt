package edu.practice11.photos

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import edu.moduleui.PracticeHeading
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.practice11.photos.data.Photos
import edu.practice11.photos.data.PhotoFileSaver
import coil.compose.AsyncImage
import edu.practice11.photos.domain.Photo
import edu.practice11.photos.presentation.ScreenModel

class MainActivity : ComponentActivity() {
    private val model by viewModels<ScreenModel> {
        viewModelFactory {
            initializer {
                val repository = Photos()
                ScreenModel(repository, PhotoFileSaver(application, repository)::save)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 6", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    PhotosScreen(model)
                }
            }
        }
    }
}

@Composable
private fun PhotosScreen(model: ScreenModel) {
    val gridState = rememberLazyGridState()
    val savePhoto = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        uri?.let(model::save)
    }
    val selected = model.selected
    val closeDetails = {
        model.message = ""
        model.selected = null
    }
    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val frame = Modifier.widthIn(max = 640.dp).fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        if (selected != null) {
            PhotoDetails(
                modifier = frame,
                photo = selected,
                canDownload = !model.busy,
                message = model.message,
                error = model.error,
                onRetry = model::retry,
                onBack = closeDetails,
                onDownload = {
                    model.message = ""
                    savePhoto.launch("photo_${selected.id}.jpg")
                },
                onImageError = { model.error = "Ошибка загрузки изображения" },
            )
        } else {
            Column(frame, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Фотокаталог",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (model.error.isNotEmpty()) PhotoError(model.error, model::retry)
                if (!model.busy && model.error.isEmpty() && model.photos.isEmpty()) {
                    Text("В каталоге нет фото", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PhotoGrid(
                    photos = model.photos,
                    state = gridState,
                    onSelect = { model.selected = it },
                    onImageError = { model.error = "Ошибка загрузки превью" },
                )
            }
        }
    }
}

@Composable
private fun PhotoDetails(
    modifier: Modifier,
    photo: Photo,
    canDownload: Boolean,
    message: String,
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onImageError: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PracticeHeading("Фото")
            TextButton(onClick = onBack) { Text("Назад") }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (error.isNotEmpty()) PhotoError(error, onRetry)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val ratio = photo.width.coerceAtLeast(1).toFloat() / photo.height.coerceAtLeast(1)
                val frameHeight = (maxWidth / ratio).coerceIn(100.dp, 320.dp)
                AsyncImage(
                    model = photo.downloadUrl,
                    contentDescription = "Фото",
                    onError = { onImageError() },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(frameHeight)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(photo.author, style = MaterialTheme.typography.titleLarge)
                Text("${photo.width}×${photo.height}", style = MaterialTheme.typography.labelLarge)
                Text(
                    photo.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        if (!canDownload) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            enabled = canDownload,
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Скачать фото") }
    }
}

@Composable
private fun PhotoError(error: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
            OutlinedButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    state: LazyGridState,
    onSelect: (Photo) -> Unit,
    onImageError: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            Surface(
                onClick = { onSelect(photo) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column {
                    AsyncImage(
                        model = photo.thumbnailUrl,
                        contentDescription = "Превью",
                        onError = { onImageError() },
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            photo.author,
                            style = MaterialTheme.typography.titleSmall,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${photo.width}×${photo.height}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

package edu.practice9

import edu.moduleui.PracticeHeading
import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryModel(application: Application) : AndroidViewModel(application) {
    val photos = mutableStateListOf<File>()
    var message by mutableStateOf("")

    private var pendingPhoto: File? = null
    private val context get() = getApplication<Application>()

    init {
        refreshPhotos()
    }

    private fun refreshPhotos() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?.listFiles()
                    .orEmpty()
                    .filter { it.extension == "jpg" && it.length() > 0 }
                    .sortedByDescending { it.name }
            }
            photos.clear()
            photos.addAll(files)
        }
    }

    fun preparePhoto(): Uri {
        val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        directory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val baseName = "IMG_$timestamp"

        var file = File(directory, "$baseName.jpg")
        var suffix = 1
        while (file.exists()) {
            file = File(directory, "${baseName}_$suffix.jpg")
            suffix++
        }

        pendingPhoto = file
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun onPictureTaken(success: Boolean) {
        if (success) {
            refreshPhotos()
        } else {
            pendingPhoto?.delete()
        }
        pendingPhoto = null
    }

    fun export(file: File) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { copyToMediaStore(file) }
                message = "Фото добавлено в галерею"
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = "Ошибка экспорта: ${error.message}"
            }
        }
    }

    private fun copyToMediaStore(file: File) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PracticeGallery")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
            "Не удалось создать фото"
        }

        try {
            checkNotNull(resolver.openOutputStream(uri)).use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            val published = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            check(resolver.update(uri, published, null, null) > 0)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}

@Composable
fun Gallery(model: GalleryModel = viewModel()) {
    var selectedPath by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPhoto = selectedPath?.let(::File)
    val snackbar = remember { SnackbarHostState() }
    val camera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = model::onPictureTaken,
    )

    LaunchedEffect(model.message) {
        if (model.message.isNotEmpty()) {
            snackbar.showSnackbar(model.message)
            model.message = ""
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (selectedPhoto != null) {
                PhotoPreview(
                    file = selectedPhoto,
                    onBack = { selectedPath = null },
                    onExport = { model.export(selectedPhoto) },
                )
            } else {
                PhotoGrid(
                    photos = model.photos,
                    onTakePhoto = {
                        try {
                            camera.launch(model.preparePhoto())
                        } catch (error: Exception) {
                            model.onPictureTaken(false)
                            model.message = "Камера недоступна: ${error.message}"
                        }
                    },
                    onSelect = { selectedPath = it.path },
                    onExport = model::export,
                )
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = if (selectedPhoto == null) 0.dp else 60.dp),
        )
    }
}

@Composable
private fun PhotoPreview(file: File, onBack: () -> Unit, onExport: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhotoHeader("Просмотр фото") {
            TextButton(onClick = onBack) { Text("Назад") }
        }
        AsyncImage(
            model = file,
            contentDescription = "Фото",
            contentScale = ContentScale.Fit,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Экспорт в галерею")
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<File>,
    onTakePhoto: () -> Unit,
    onSelect: (File) -> Unit,
    onExport: (File) -> Unit,
) {
    PhotoHeader("Фотогалерея") {
        if (photos.isNotEmpty()) {
            Button(onClick = onTakePhoto) { Text("Сделать фото") }
        }
    }
    if (photos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "У вас пока нет фото",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onTakePhoto) { Text("Сделать первое фото") }
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(photos, key = { it.name }) { photo ->
            PhotoTile(photo, onSelect = { onSelect(photo) }, onExport = { onExport(photo) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoHeader(title: String, action: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)) { PracticeHeading(title) }
        Box(Modifier.align(Alignment.CenterVertically)) { action() }
    }
}

@Composable
private fun PhotoTile(file: File, onSelect: () -> Unit, onExport: () -> Unit) {
    val dots = MaterialTheme.colorScheme.inverseOnSurface
    var showMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.small)) {
        AsyncImage(
            model = file,
            contentDescription = file.name,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onSelect),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.align(Alignment.TopEnd)) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = "Действия для ${file.name}"
                },
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f)) {
                    Canvas(Modifier.size(28.dp)) {
                        for (offset in -1..1) {
                            drawCircle(dots, 1.5.dp.toPx(), Offset(center.x, center.y + offset * 5.dp.toPx()))
                        }
                    }
                }
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Экспорт в галерею") },
                    onClick = {
                        showMenu = false
                        onExport()
                    },
                )
            }
        }
    }
}

package edu.practice8

import edu.moduleui.PracticeHeading
import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Note(
    val file: String,
    val title: String,
    val body: String,
    val date: Long,
)

class DiaryModel(application: Application) : AndroidViewModel(application) {
    val notes = mutableStateListOf<Note>()

    var editing by mutableStateOf(false)
        private set

    var title by mutableStateOf("")
    var body by mutableStateOf("")

    var error by mutableStateOf("")
        private set

    private var currentNote: Note? = null
    private val directory get() = getApplication<Application>().filesDir

    init {
        viewModelScope.launch {
            val savedNotes = withContext(Dispatchers.IO) {
                directory.listFiles()
                    .orEmpty()
                    .filter { it.extension == "txt" }
                    .map(::readNote)
                    .sortedByDescending { it.date }
            }
            notes.addAll(savedNotes)
        }
    }

    fun open(note: Note?) {
        currentNote = note
        title = note?.title.orEmpty()
        body = note?.body.orEmpty()
        editing = true
    }

    fun closeEditor() {
        editing = false
    }

    fun save() {
        val existingName = currentNote?.file
        val titleToSave = title
        val bodyToSave = body

        viewModelScope.launch {
            try {
                val note = withContext(Dispatchers.IO) {
                    val file = if (existingName == null) createNoteFile(titleToSave)
                    else File(directory, existingName)
                    val titleLine = titleToSave.replace('\n', ' ')
                    try {
                        file.writeText("$titleLine\n$bodyToSave")
                    } catch (failure: IOException) {
                        if (existingName == null) file.delete()
                        throw failure
                    }
                    Note(file.name, titleToSave, bodyToSave, file.lastModified())
                }
                val index = notes.indexOfFirst { it.file == note.file }
                if (index < 0) {
                    notes.add(0, note)
                } else {
                    notes[index] = note
                }
                closeEditor()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                error = "Ошибка записи: ${failure.message}"
            }
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    check(File(directory, note.file).delete())
                }
                notes.removeAll { it.file == note.file }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                error = "Ошибка удаления: ${failure.message}"
            }
        }
    }

    private fun createNoteFile(title: String): File {
        val safeTitle = title.replace(Regex("""[^\p{L}\p{N}-]+"""), "_").trim('_').take(60)
        val suffix = if (safeTitle.isEmpty()) "" else "_$safeTitle"
        var timestamp = System.currentTimeMillis()
        var file = File(directory, "$timestamp$suffix.txt")
        while (!file.createNewFile()) {
            timestamp++
            file = File(directory, "$timestamp$suffix.txt")
        }
        return file
    }

    private fun readNote(file: File): Note {
        val lines = file.readText().split("\n", limit = 2)
        return Note(
            file = file.name,
            title = lines[0],
            body = lines.getOrElse(1) { "" },
            date = file.lastModified(),
        )
    }
}

@Composable
fun Diary(model: DiaryModel = viewModel()) {
    Column(Modifier.fillMaxSize()) {
        if (model.error.isNotBlank()) {
            Text(
                model.error,
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (model.editing) {
            NoteEditor(model)
        } else {
            NoteList(model)
        }
    }
}

@Composable
private fun NoteEditor(model: DiaryModel) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(bottom = 8.dp)) { PracticeHeading("Запись дневника") }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = model.title,
                onValueChange = { model.title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Заголовок (необязательно)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = model.body,
                onValueChange = { model.body = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Запись") },
                minLines = 5,
                maxLines = 12,
            )
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = model::closeEditor) { Text("Назад") }
            Button(onClick = model::save) { Text("Сохранить") }
        }
    }
}

@Composable
private fun NoteList(model: DiaryModel) {
    Box(Modifier.fillMaxSize()) {
        if (model.notes.isEmpty()) {
            Column(
                Modifier.align(Alignment.Center).padding(bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "У вас пока нет записей",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Нажмите +, чтобы создать первую",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(model.notes, key = { it.file }) { note ->
                    NoteRow(note, onOpen = { model.open(note) }, onDelete = { model.delete(note) })
                    HorizontalDivider()
                }
            }
        }

        ExtendedFloatingActionButton(
            text = { Text("Новая запись") },
            icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
            onClick = { model.open(null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(note: Note, onOpen: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = { showMenu = true })
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            note.title.ifBlank { "Запись" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(note.body.take(40), style = MaterialTheme.typography.bodyMedium)
        Text(
            DateFormat.getDateTimeInstance().format(Date(note.date)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

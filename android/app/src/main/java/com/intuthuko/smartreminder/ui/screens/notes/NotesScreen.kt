package com.intuthuko.smartreminder.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.navigation.Screen
import com.intuthuko.smartreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(user: User, navController: NavController) {
    val repository = remember { AppRepository() }
    var notes by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getNotesForUser(user.userId)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    val subjects = remember(notes) { listOf("All") + notes.map { it.subjectName }.distinct().sorted() }
    val filteredNotes = remember(notes, searchQuery, selectedSubject) {
        var result = notes
        if (selectedSubject != "All") result = result.filter { it.subjectName == selectedSubject }
        if (searchQuery.isNotBlank()) result = result.filter { it.title.contains(searchQuery, ignoreCase = true) || it.body.contains(searchQuery, ignoreCase = true) }
        result
    }
    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val otherNotes = filteredNotes.filter { !it.isPinned }

    Scaffold(
        topBar = { AppHeader("Notes", "${notes.size} ideas, all in one place.", Icons.Default.NoteAlt) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingNote = null; showEditor = true }, containerColor = CobaltBlue, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(22.dp)) {
                Icon(Icons.Default.Add, "Add Note")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite, cursorColor = CobaltBlue)
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { subj ->
                        FilterChipItem(label = subj, selected = selectedSubject == subj, onClick = { selectedSubject = subj })
                    }
                }
            }
            if (filteredNotes.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.NoteAdd, title = "No notes yet", subtitle = "Tap + to create your first note")
                }
            } else {
                if (pinnedNotes.isNotEmpty()) {
                    item { Text("Pinned", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecondaryText) }
                    items(pinnedNotes, key = { it.noteId }) { note -> NoteCard(note, onEdit = { editingNote = note; showEditor = true }, onDelete = { noteToDelete = note }) }
                }
                if (otherNotes.isNotEmpty()) {
                    item { Text("Recent", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecondaryText) }
                    items(otherNotes, key = { it.noteId }) { note -> NoteCard(note, onEdit = { editingNote = note; showEditor = true }, onDelete = { noteToDelete = note }) }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditor(note = editingNote, userId = user.userId, onDismiss = { showEditor = false }, onSaved = { notes = repository.getNotesForUser(user.userId); showEditor = false })
    }
    noteToDelete?.let { note ->
        ConfirmDialog(message = "Delete '${note.title}'?", onConfirm = { repository.deleteNote(note.noteId); notes = repository.getNotesForUser(user.userId); noteToDelete = null }, onDismiss = { noteToDelete = null }, confirmText = "Delete")
    }
}

@Composable
fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) { Icon(Icons.Default.PushPin, null, tint = WarningAmber, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)) }
                    Text(note.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(16.dp)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete", color = Danger) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Danger) }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }
            if (note.subjectName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = subjectColor(note.subjectName).copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text(note.subjectName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subjectColor(note.subjectName))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(note.body, fontSize = 13.sp, color = SecondaryText, maxLines = 3)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isOfflineAvailable) { Icon(Icons.Default.CloudDone, null, tint = SuccessGreen, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(4.dp)) }
                Text(note.updatedAt, fontSize = 11.sp, color = Gray400)
            }
        }
    }
}

@Composable
fun NoteEditor(note: Note?, userId: Int, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val repository = remember { AppRepository() }
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    var subjectName by remember { mutableStateOf(note?.subjectName ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }

    AppDialog(title = if (note != null) "Edit Note" else "New Note", onDismiss = onDismiss) {
        OutlinedTextField(value = title, onValueChange = { title = it.take(160) }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = subjectName, onValueChange = { subjectName = it.take(160) }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = body, onValueChange = { body = it.take(8000) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPinned, onCheckedChange = { isPinned = it }, colors = CheckboxDefaults.colors(checkedColor = CobaltBlue))
            Text("Pin note", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(18.dp)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (title.isNotBlank()) {
                    if (note != null) repository.updateNote(note.copy(title = title, body = body, subjectName = subjectName, isPinned = isPinned, updatedAt = java.time.LocalDate.now().toString()))
                    else repository.addNote(Note(0, userId, title, body, subjectName, isPinned, updatedAt = java.time.LocalDate.now().toString()))
                    onSaved()
                }
            }, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

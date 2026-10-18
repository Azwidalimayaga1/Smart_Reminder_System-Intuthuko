package com.intuthuko.smartreminder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intuthuko.smartreminder.data.CloudStore
import com.intuthuko.smartreminder.data.LocalStore
import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.ui.screens.admin.AddSubjectDialog

@Composable
fun CloudStatus(user: User) {
    if (user.authUid.isEmpty()) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CloudDone, null, Modifier.size(16.dp))
            Text(CloudStore.status, style = MaterialTheme.typography.labelSmall)
        }
    }
    CloudStore.error?.let { message ->
        AlertDialog(onDismissRequest = { CloudStore.dismissError() }, title = { Text("Cloud sync") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { CloudStore.dismissError() }) { Text("Got it") } })
    }
}

@Composable
fun PublishUpdateDialog(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    AppDialog("Campus update", onDismiss) {
        Text("This update will be visible to everyone signed in to CampusSync.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(title, { title = it.take(160) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(message, { message = it.take(5000) }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = { CloudStore.publishAnnouncement(title, message); onDismiss() },
            enabled = title.isNotBlank() && message.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Publish update") }
    }
}

@Composable
fun CloudAdminPanel(onBack: () -> Unit) {
    val revision = LocalStore.revision
    val subjects = remember(revision) { MockData.subjects.toList() }
    var add by remember { mutableStateOf(false) }
    var publish by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Subject?>(null) }
    Scaffold(topBar = { AppHeader("Campus administration", "Manage the shared subject catalogue.", Icons.Default.AdminPanelSettings, onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Private student notes and reminders stay private. Staff roles are assigned by the project owner in Firebase.", style = MaterialTheme.typography.bodyMedium) }
            item { Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Text("Add subject") } }
            item { OutlinedButton(onClick = { publish = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Campaign, null); Spacer(Modifier.width(8.dp)); Text("Publish campus update") } }
            if (subjects.isEmpty()) item { Text("No subjects yet. Add your campus subjects to make them available for enrolment.") }
            items(subjects, key = { it.subjectId }) { subject ->
                ListItem(headlineContent = { Text(subject.subjectName) }, supportingContent = { Text("R%.2f per month".format(subject.monthlyFee)) },
                    trailingContent = { IconButton(onClick = { deleting = subject }) { Icon(Icons.Default.Delete, "Delete subject") } })
            }
        }
    }
    if (add) AddSubjectDialog(onDismiss = { add = false }, onCreated = { add = false })
    if (publish) PublishUpdateDialog { publish = false }
    deleting?.let { subject -> ConfirmDialog("Remove ${subject.subjectName} from the subject catalogue?", onConfirm = { CloudStore.deleteSubject(subject.subjectId); deleting = null }, onDismiss = { deleting = null }, confirmText = "Remove") }
}

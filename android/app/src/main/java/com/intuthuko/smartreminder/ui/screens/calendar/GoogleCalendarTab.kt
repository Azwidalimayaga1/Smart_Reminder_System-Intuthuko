package com.intuthuko.smartreminder.ui.screens.calendar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intuthuko.smartreminder.calendar.GoogleCalendarViewModel
import com.intuthuko.smartreminder.data.model.User
import com.intuthuko.smartreminder.ui.components.EmptyState
import com.intuthuko.smartreminder.ui.components.Panel
import com.intuthuko.smartreminder.ui.theme.*

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun GoogleCalendarTab(user: User) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val model: GoogleCalendarViewModel = viewModel(key = "google-calendar-${user.authUid}-${user.userId}")
    var showDisconnect by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (activity != null) model.onPermissionResult(activity, user, result.resultCode, result.data)
    }
    val connect = {
        if (activity != null) model.connect(activity, user) { launcher.launch(it) }
    }

    LazyColumn(Modifier.fillMaxSize().background(WarmBackground), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = LightBlue, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.EventAvailable, null, tint = CobaltBlue, modifier = Modifier.padding(12.dp).size(26.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Your Google Calendar", style = MaterialTheme.typography.titleLarge, color = MainText)
                        Text(if (model.connected) user.email else "Your plans, together in one view", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }
                Text("View up to 20 upcoming events from your primary Google calendar. CampusSync only reads events; it cannot create, change or delete them.",
                    style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                if (user.authUid.isEmpty()) {
                    Text("Sign in with your real campus account to connect. Local demo accounts cannot access Google Calendar.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
                if (model.connected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = connect, enabled = !model.busy && activity != null, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Refresh")
                        }
                        OutlinedButton(onClick = { showDisconnect = true }, enabled = !model.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 50.dp)) { Text("Disconnect") }
                    }
                    model.updatedAt?.let { Text("Last updated $it • Times shown in your phone’s timezone", style = MaterialTheme.typography.labelSmall, color = SecondaryText) }
                } else {
                    Button(onClick = connect, enabled = !model.busy && user.authUid.isNotEmpty() && activity != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.Link, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Connect Google Calendar")
                    }
                    Text("Google will ask you to allow calendar access separately from signing in. Connect the Google account that matches your campus login email.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
            }
        }
        if (model.busy) item {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Text("Connecting to Google Calendar…", style = MaterialTheme.typography.bodyMedium, color = MainText)
            }
        }
        model.error?.let { error -> item {
            Surface(color = HighPriorityBackground, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, null, tint = Danger)
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = MainText)
                }
            }
        } }
        if (model.connected) {
            item { Text("Upcoming events", style = MaterialTheme.typography.titleLarge, color = MainText) }
            if (model.events.isEmpty() && !model.busy && model.error == null) item {
                EmptyState(Icons.Default.EventNote, "Your calendar is clear", "No upcoming events were found in your primary Google calendar. Add an event in Google Calendar, then tap Refresh.")
            }
            items(model.events, key = { it.id }) { event ->
                Panel {
                    Text(event.title, style = MaterialTheme.typography.titleMedium, color = MainText)
                    Text(event.displayWhen(), style = MaterialTheme.typography.bodyMedium, color = CobaltBlue)
                    if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
            }
        }
        item {
            Text("Google events stay separate from your CampusSync calendar and phone reminders. This view is cleared when you disconnect or sign out; it is not uploaded to Firebase.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
    }
    if (showDisconnect) AlertDialog(
        onDismissRequest = { showDisconnect = false },
        title = { Text("Disconnect this calendar?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("This clears Google events from CampusSync and keeps your campus account signed in. It does not delete Google events. Google’s existing permission remains until you remove it in your Google Account.")
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/connections")))
            }) { Text("Manage Google permissions") }
        } },
        confirmButton = { TextButton(onClick = { model.disconnect(); showDisconnect = false }) { Text("Disconnect") } },
        dismissButton = { TextButton(onClick = { showDisconnect = false }) { Text("Cancel") } }
    )
}

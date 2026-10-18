package com.intuthuko.smartreminder.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.intuthuko.smartreminder.data.model.User
import com.intuthuko.smartreminder.reminders.ReminderScheduler

@Composable
fun ReminderPermissionPrompt(user: User) {
    val context = LocalContext.current
    var prompt by remember(user.userId) { mutableStateOf(!ReminderScheduler.canNotify(context) && !ReminderScheduler.permissionAsked(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        ReminderScheduler.restore(context); ReminderScheduler.dataChanged()
    }
    if (prompt) AlertDialog(
        onDismissRequest = { prompt = false; ReminderScheduler.markPermissionAsked(context) },
        title = { Text("Stay ahead of deadlines") },
        text = { Text("Allow CampusSync to remind you about assignments and calendar events, even when the app is closed. You can change this in Settings.") },
        confirmButton = { TextButton(onClick = {
            prompt = false; ReminderScheduler.markPermissionAsked(context)
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Enable reminders") } },
        dismissButton = { TextButton(onClick = { prompt = false; ReminderScheduler.markPermissionAsked(context) }) { Text("Not now") } }
    )
}

@Composable
fun ReminderSettingsPanel(user: User) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revision by remember { mutableIntStateOf(0) }
    var enabled by remember(user.userId) { mutableStateOf(ReminderScheduler.enabled(context, user)) }
    var notice by remember { mutableStateOf<String?>(null) }
    val allowed = remember(revision) { ReminderScheduler.canNotify(context) }
    val precise = remember(revision) { ReminderScheduler.exact(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        revision++; ReminderScheduler.restore(context); ReminderScheduler.dataChanged()
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { revision++; ReminderScheduler.restore(context) } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderScheduler.permissionAsked(context)) {
            ReminderScheduler.markPermissionAsked(context); launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("Phone reminders")
        Panel {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Keep me on track", style = MaterialTheme.typography.titleSmall)
                    Text(if (!enabled) "Reminders are switched off" else if (allowed) "Alerts can appear while CampusSync is closed" else "Allow notifications on this phone to receive alerts", style = MaterialTheme.typography.bodySmall)
                }
                Switch(enabled, { enabled = it; ReminderScheduler.setEnabled(context, user, it); if (it && !allowed) requestNotifications() })
            }
            Text("Assignments: 9:00 AM on the due date. Calendar events: 30 minutes before the start. Events without a time use 9:00 AM. Times follow your phone's time zone.", style = MaterialTheme.typography.bodySmall)
            if (!allowed) OutlinedButton(onClick = { requestNotifications() }, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
            if (Build.VERSION.SDK_INT >= 31 && !precise) {
                Text("Android may delay reminders unless precise timing is allowed.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }
                        .onFailure { notice = "Open Android Settings → Apps → CampusSync → Alarms and reminders to allow precise timing." }
                }, modifier = Modifier.fillMaxWidth()) { Text("Allow precise timing") }
            }
            Button(onClick = {
                notice = if (ReminderScheduler.scheduleTest(context, user)) {
                    if (precise) "Test scheduled for 20 seconds from now. Go to your phone's home screen and wait for the alert."
                    else "Test scheduled. Go to your phone's home screen. Android may delay the alert; allow precise timing for a reliable 20-second test."
                } else "Switch reminders on and allow notifications first."
            }, enabled = enabled && allowed, modifier = Modifier.fillMaxWidth()) { Text("Test a phone reminder") }
            Text("Reminders already loaded on this phone work without internet. Open CampusSync to load changes made on another device. Android force-stop pauses alerts until you open the app again.", style = MaterialTheme.typography.bodySmall)
        }
    }
    notice?.let { AlertDialog(onDismissRequest = { notice = null }, title = { Text("Phone reminders") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { notice = null }) { Text("Got it") } }) }
}

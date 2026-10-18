package com.intuthuko.smartreminder.ui.screens.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.LocalStore
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*
@Composable
fun SettingsScreen(navController: NavController, user: User) {
    var theme by remember { mutableStateOf(LocalStore.prefs.getString("theme", "device")!!) }
    var password by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { AppHeader("Settings", "A space that feels like you.", Icons.Default.Tune, onBack = { navController.popBackStack() }) }, containerColor = WarmBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Appearance")
                Panel {
                    Text("Choose your view", style = MaterialTheme.typography.titleSmall, color = MainText)
                    listOf("light" to "Light theme", "dark" to "Dark theme", "device" to "Use device setting").forEach { (value, label) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { theme = value; LocalStore.prefs.edit().putString("theme", value).apply(); LocalStore.save() }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(when(value) { "light" -> Icons.Default.LightMode; "dark" -> Icons.Default.DarkMode; else -> Icons.Default.PhoneAndroid }, null, tint = if (theme == value) CobaltBlue else SecondaryText)
                            Text(label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), color = MainText, style = MaterialTheme.typography.bodyMedium)
                            RadioButton(selected = theme == value, onClick = { theme = value; LocalStore.prefs.edit().putString("theme", value).apply(); LocalStore.save() })
                        }
                    }
                }
            }
            ReminderSettingsPanel(user)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Inbox & reminder categories")
                Panel {
                    NotificationCategory.entries.forEachIndexed { index, category ->
                        val key = "notification_${user.userId}_${category.name}"
                        var enabled by remember(key) { mutableStateOf(LocalStore.prefs.getBoolean(key, true)) }
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) { Text(category.displayName, Modifier.weight(1f), color = MainText, style = MaterialTheme.typography.bodyMedium); Switch(checked = enabled, onCheckedChange = { enabled = it; LocalStore.prefs.edit().putBoolean(key, it).apply(); LocalStore.save() }) }
                        if (index < NotificationCategory.entries.lastIndex) Divider(color = BorderColor.copy(alpha = 0.6f))
                    }
                    Text("Choose your inbox updates and phone reminder categories.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Account & storage")
                MenuRow("Change password", "Keep your account secure", Icons.Default.Lock) { password = true }
                MenuRow("Save offline data", "Stored on this device", Icons.Default.OfflinePin, SuccessGreen) { LocalStore.save(); notice = if (com.intuthuko.smartreminder.data.CloudStore.active) com.intuthuko.smartreminder.data.CloudStore.status else "Your demo changes are saved on this device." }
            }
            Text("English • CampusSync 1.0", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
    }
    if (password) ChangePasswordDialog(user) { password = false }
    notice?.let { AlertDialog(onDismissRequest = { notice = null }, title = { Text("All saved") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { notice = null }) { Text("Got it") } }) }
}

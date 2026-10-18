package com.intuthuko.smartreminder.ui.screens.more
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.LocalStore
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.navigation.Screen
import com.intuthuko.smartreminder.ui.theme.*
@Composable
fun MoreScreen(user: User, navController: NavController, onLogout: () -> Unit) {
    var publish by remember { mutableStateOf(false) }
    var logout by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(WarmBackground), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Your space", style = MaterialTheme.typography.headlineLarge, color = MainText); Text("The little details that keep life in sync.", style = MaterialTheme.typography.bodyMedium, color = SecondaryText) }
        item {
            Surface(onClick = { navController.navigate(Screen.Profile.route) }, color = MidnightNavy, shape = RoundedCornerShape(26.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AvatarInitials(user.username, 54, androidx.compose.ui.graphics.Color(0xFF087F72))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(user.username, style = MaterialTheme.typography.titleLarge, color = Color.White); Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC6D8DC)); Text(user.role.displayName, style = MaterialTheme.typography.labelSmall, color = HeroMint) }
                    Icon(Icons.Default.ChevronRight, "My Profile", tint = HeroMint)
                }
            }
        }
        item {
            SectionHeading(if (user.role == UserRole.Student) "Learning essentials" else "Workspace tools")
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (user.role == UserRole.Student) MenuRow("My subjects", "Your classes and enrolments", Icons.Default.MenuBook) { navController.navigate(Screen.Subjects.route) }
                if (user.authUid.isNotEmpty() && user.role in listOf(UserRole.Tutor, UserRole.Admin)) MenuRow("Publish campus update", "Share news with your campus", Icons.Default.Campaign) { publish = true }
                if (user.role == UserRole.Student) MenuRow("GPA calculator", "Track your academic progress", Icons.Default.Calculate, PurpleAccent) { navController.navigate(Screen.GPACalculator.route) }
                if (user.role != UserRole.Student) {
                    MenuRow("Personal tasks", "Your own deadlines and follow-ups", Icons.Default.Checklist) { navController.navigate(Screen.Assignments.route) }
                    MenuRow("Personal calendar", "Plan meetings and reminders", Icons.Default.CalendarMonth) { navController.navigate(Screen.Calendar.route) }
                    MenuRow("Private notes", "Ideas and preparation, just for you", Icons.Default.NoteAlt, PurpleAccent) { navController.navigate(Screen.Notes.route) }
                }
                MenuRow("Campus updates", "News from your campus", Icons.Default.Campaign) { navController.navigate(Screen.Updates.route) }
                MenuRow("Notifications", "Updates worth knowing", Icons.Default.NotificationsNone, WarningAmber) { navController.navigate(Screen.Notifications.route) }
                if (user.role == UserRole.Admin) MenuRow("Admin panel", "Manage your campus community", Icons.Default.AdminPanelSettings, PurpleAccent) { navController.navigate(Screen.Admin.route) }
            }
        }
        item {
            SectionHeading("Make it yours")
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MenuRow("My profile", "Personal and study details", Icons.Default.PersonOutline) { navController.navigate(Screen.Profile.route) }
                MenuRow("Settings", "Appearance and preferences", Icons.Default.Tune, PurpleAccent) { navController.navigate(Screen.Settings.route) }
                MenuRow("Save offline data", "Keep your changes on this device", Icons.Default.OfflinePin, SuccessGreen) { LocalStore.save(); info = if (com.intuthuko.smartreminder.data.CloudStore.active) com.intuthuko.smartreminder.data.CloudStore.status else "Your demo changes are saved on this device." }
                MenuRow("About CampusSync", "A calmer campus life", Icons.Default.Info, WarningAmber) { info = "CampusSync 1.0\n\nBuilt for the Intuthuko College community. Plan assignments, collect ideas, manage your subjects and keep your learning in sync.\n\nGoogle and email accounts sync personal records through Firebase. Demo accounts stay on this device." }
            }
        }
        item { OutlinedButton(onClick = { logout = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Sign out") } }
        item { Text("CampusSync 1.0 • Made for your next chapter", color = SecondaryText, style = MaterialTheme.typography.bodySmall) }
    }
    if (publish) PublishUpdateDialog { publish = false }
    if (logout) ConfirmDialog("Sign out of your study space? Your saved data will stay on this device.", onConfirm = { logout = false; onLogout() }, onDismiss = { logout = false }, confirmText = "Sign out")
    info?.let { AlertDialog(onDismissRequest = { info = null }, title = { Text("CampusSync") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { info = null }) { Text("Got it") } }) }
}

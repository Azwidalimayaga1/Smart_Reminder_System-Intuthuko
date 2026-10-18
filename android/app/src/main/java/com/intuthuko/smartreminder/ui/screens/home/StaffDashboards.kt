package com.intuthuko.smartreminder.ui.screens.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.CloudStore
import com.intuthuko.smartreminder.data.LocalStore
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.navigation.Screen
import com.intuthuko.smartreminder.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TutorInk = Color(0xFF302952)
private val TutorGlow = Color(0xFFE1D6FF)
private val AdminInk = Color(0xFF143449)
private val AdminGlow = Color(0xFFBFE3FF)

@Composable
internal fun TutorDashboard(user: User, nav: NavController) {
    val revision = LocalStore.revision
    val repo = remember(revision) { AppRepository() }
    val today = LocalDate.now().toString()
    val events = repo.getEventsForUser(user.userId).filter { it.eventDate >= today }
        .sortedWith(compareBy({ it.eventDate }, { it.eventTime ?: "09:00" }))
    val tasks = repo.getTasksForUser(user.userId).filter { it.status == TaskStatus.Pending }
    val notes = repo.getNotesForUser(user.userId)
    var publish by remember { mutableStateOf(false) }
    var demoNotice by remember { mutableStateOf(false) }
    val openPublisher = { if (CloudStore.active) publish = true else demoNotice = true }

    LazyColumn(Modifier.fillMaxSize().background(WarmBackground),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { StaffHeader("Teaching workspace", "TUTOR", user, PurpleAccent, nav) }
        item {
            GradientPanel(TutorInk, Color(0xFF51416E)) {
                Text("PREPARE. CONNECT. INSPIRE.", color = TutorGlow, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.3.sp)
                Text("Ready for your\nnext great session?", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Welcome, ${user.username.substringBefore(' ')}. Keep your preparation and campus communication in one place.", color = Color(0xFFE6E0F0), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = openPublisher, colors = ButtonDefaults.buttonColors(containerColor = TutorGlow, contentColor = TutorInk),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.heightIn(min = 50.dp)) {
                    Icon(Icons.Default.Campaign, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Publish campus update")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StaffMetric("Today’s events", "${events.count { it.eventDate == today }}", Icons.Default.Today, PurpleAccent, Modifier.weight(1f))
                StaffMetric("Personal tasks", "${tasks.size}", Icons.Default.Checklist, CobaltBlue, Modifier.weight(1f))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Your agenda", action = { TextButton(onClick = { nav.navigate(Screen.Calendar.route) }) { Text("Open planner") } })
                Text("Your personal events and meetings", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                if (events.isEmpty()) {
                    MenuRow("Make room for what’s next", "Add a session or meeting to your calendar", Icons.Default.EventAvailable, PurpleAccent) { nav.navigate(Screen.Calendar.route) }
                } else events.take(3).forEach { event ->
                    Surface(onClick = { nav.navigate(Screen.Calendar.route) }, color = SurfaceWhite, shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(color = PurpleLight, shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(runCatching { LocalDate.parse(event.eventDate).dayOfMonth.toString() }.getOrDefault("—"), style = MaterialTheme.typography.titleLarge, color = PurpleAccent)
                                    Text(runCatching { LocalDate.parse(event.eventDate).format(DateTimeFormatter.ofPattern("MMM")) }.getOrDefault(""), style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(event.title, style = MaterialTheme.typography.titleSmall, color = MainText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${event.eventTime ?: "All day"} • ${event.eventType.displayName}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = SecondaryText)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Preparation desk")
                MenuRow("Private teaching notes", "${notes.size} saved • Lesson ideas and preparation", Icons.Default.EditNote, PurpleAccent) { nav.navigate(Screen.Notes.route) }
                MenuRow("Personal follow-ups", "${tasks.size} pending • Organise your own deadlines", Icons.Default.Assignment, CobaltBlue) { nav.navigate(Screen.Assignments.route) }
            }
        }
        item {
            SectionHeading("Campus conversation")
            MenuRow("Read campus updates", "Stay connected with your community", Icons.Default.Forum, PurpleAccent) { nav.navigate(Screen.Updates.route) }
        }
    }
    if (publish) PublishUpdateDialog { publish = false }
    if (demoNotice) DemoPublishingNotice { demoNotice = false }
}

@Composable
internal fun AdminDashboard(user: User, nav: NavController) {
    val revision = LocalStore.revision
    val repo = remember(revision) { AppRepository() }
    val subjects = repo.getAllSubjects()
    val updates = repo.getAnnouncements()
    val timetable = repo.getTimetableEntries()
    var publish by remember { mutableStateOf(false) }
    var demoNotice by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(WarmBackground),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { StaffHeader("Campus overview", "ADMIN", user, CobaltBlue, nav) }
        item {
            GradientPanel(AdminInk, Color(0xFF20566B)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("CAMPUS OPERATIONS", color = AdminGlow, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.AdminPanelSettings, null, tint = AdminGlow, modifier = Modifier.size(30.dp))
                }
                Text("A connected campus\nstarts here.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Manage your shared catalogue and keep your community informed.", color = Color(0xFFD8E8EF), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { nav.navigate(Screen.Admin.route) }, colors = ButtonDefaults.buttonColors(containerColor = AdminGlow, contentColor = AdminInk),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.heightIn(min = 50.dp)) {
                    Text("Open campus management"); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Shared campus at a glance")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StaffMetric("Subjects", "${subjects.size}", Icons.Default.MenuBook, CobaltBlue, Modifier.weight(1f))
                    StaffMetric("Visible updates", "${updates.size}", Icons.Default.Campaign, PurpleAccent, Modifier.weight(1f))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Administration")
                MenuRow("Subject catalogue", "Add and manage campus subjects", Icons.Default.LibraryBooks, CobaltBlue) { nav.navigate(Screen.Admin.route) }
                MenuRow("Campus communications", "Publish an update for everyone", Icons.Default.Campaign, PurpleAccent) { if (CloudStore.active) publish = true else demoNotice = true }
                MenuRow("Personal planner", "Your meetings and operational reminders", Icons.Default.EventNote, WarningAmber) { nav.navigate(Screen.Calendar.route) }
            }
        }
        item {
            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, null, tint = SuccessGreen)
                    Text("Privacy by design", style = MaterialTheme.typography.titleSmall, color = MainText)
                }
                Text("Personal notes, tasks and reminders belong to each account. This overview shows shared campus content.", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                Text("${timetable.size} shared timetable entries available", style = MaterialTheme.typography.labelMedium, color = CobaltBlue)
            }
        }
        item {
            SectionHeading("Latest campus update", action = { TextButton(onClick = { nav.navigate(Screen.Updates.route) }) { Text("View all") } })
        }
        if (updates.isEmpty()) item {
            MenuRow("Start the conversation", "Publish your first campus update", Icons.Default.AddComment, PurpleAccent) { if (CloudStore.active) publish = true else demoNotice = true }
        } else item { UpdateCard(updates.first()) }
    }
    if (publish) PublishUpdateDialog { publish = false }
    if (demoNotice) DemoPublishingNotice { demoNotice = false }
}

@Composable
private fun StaffHeader(title: String, label: String, user: User, accent: Color, nav: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampusSyncLogo(40)
            Column(Modifier.weight(1f)) {
                Text("CampusSync", style = MaterialTheme.typography.titleSmall, color = MainText)
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM")), style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            }
            IconButton(onClick = { nav.navigate(Screen.Notifications.route) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.NotificationsNone, "Notifications", tint = MainText) }
            Surface(onClick = { nav.navigate(Screen.Profile.route) }, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .12f)) {
                Text(user.username.trim().take(1).uppercase(), color = accent, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).wrapContentSize(Alignment.Center))
            }
        }
        Text(label, color = accent, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MainText, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun GradientPanel(start: Color, end: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = start) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(start, end))).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
    }
}

@Composable
private fun StaffMetric(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier) {
    Panel(modifier) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MainText)
        Text(label, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
    }
}

@Composable
fun CampusUpdatesScreen(user: User) {
    val revision = LocalStore.revision
    val updates = remember(revision) { AppRepository().getAnnouncements() }
    val staff = user.role == UserRole.Tutor || user.role == UserRole.Admin
    var publish by remember { mutableStateOf(false) }
    var demoNotice by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(WarmBackground), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Campus updates", style = MaterialTheme.typography.headlineLarge, color = MainText, modifier = Modifier.semantics { heading() })
            Text("One community. Keep everyone connected.", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
        }
        if (staff) item {
            Button(onClick = { if (CloudStore.active) publish = true else demoNotice = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Campaign, null); Spacer(Modifier.width(8.dp)); Text("Publish campus update")
            }
        }
        if (updates.isEmpty()) item { EmptyState(Icons.Default.Forum, "A fresh start", if (staff) "Share your first campus update with the community." else "Campus news will appear here when staff publish an update.") }
        items(updates, key = { it.announcementId }) { UpdateCard(it) }
    }
    if (publish && staff) PublishUpdateDialog { publish = false }
    if (demoNotice) DemoPublishingNotice { demoNotice = false }
}

@Composable
private fun UpdateCard(update: Announcement) {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Campaign, null, tint = PurpleAccent)
            Text("CAMPUS UPDATE", style = MaterialTheme.typography.labelSmall, color = PurpleAccent, letterSpacing = 1.sp)
        }
        Text(update.title, style = MaterialTheme.typography.titleMedium, color = MainText)
        Text(update.message, style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
        if (update.timestamp.isNotBlank()) Text(update.timestamp, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
    }
}

@Composable
private fun DemoPublishingNotice(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Use your campus account") },
        text = { Text("Sign in with a real Tutor or Admin account to publish shared campus updates. This demo account only uses data on this phone.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } })
}

package com.intuthuko.smartreminder.ui.screens.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.LocalStore
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.navigation.Screen
import com.intuthuko.smartreminder.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, navController: NavController) {
    when (user.role) {
        UserRole.Student -> StudentDashboard(user, navController)
        UserRole.Tutor -> TutorDashboard(user, navController)
        UserRole.Admin -> AdminDashboard(user, navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDashboard(user: User, navController: NavController) {
    val revision = LocalStore.revision
    val repo = remember(revision) { AppRepository() }
    val profile = repo.getUser(user.userId) ?: user
    val tasks = remember(revision, user) { repo.getTasksForUser(user.userId) }
    val pending = tasks.filter { it.status == TaskStatus.Pending }.sortedBy { it.dueDate }
    val complete = tasks.count { it.status == TaskStatus.Completed || it.status == TaskStatus.Submitted }
    val rate = if (tasks.isEmpty()) 0f else complete.toFloat() / tasks.size
    val today = LocalDate.now().toString()
    val overdue = pending.count { it.dueDate < today }
    val announcements = remember(revision) { repo.getAnnouncements() }
    val greeting = when(LocalTime.now().hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    LazyColumn(Modifier.fillMaxSize().background(WarmBackground), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampusSyncLogo(44)
                Column(Modifier.weight(1f)) {
                    Text("CampusSync", style = MaterialTheme.typography.titleMedium, color = MainText)
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
                BadgedBox(badge = { val count = repo.getUnreadNotificationCount(user.userId); if (count > 0) Badge(containerColor = Danger) { Text("$count") } }) {
                    FilledTonalIconButton(onClick = { navController.navigate(Screen.Notifications.route) }, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceWhite, contentColor = MainText)) { Icon(Icons.Default.NotificationsNone, "Notifications") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MidnightNavy), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("STUDENT • YOUR LEARNING, IN SYNC", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, color = HeroMint, modifier = Modifier.weight(1f))
                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) { Text(profile.role.displayName, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
                    }
                    Text("$greeting,\n${profile.username.substringBefore(" ")}.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text(if (overdue > 0) "$overdue overdue ${if (overdue == 1) "assignment needs" else "assignments need"} your attention. Let's make progress." else "A clear mind starts with a clear plan. Make today count.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC6D8DC))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = rate, color = HeroMint, trackColor = Color.White.copy(alpha = 0.12f), strokeWidth = 5.dp, modifier = Modifier.fillMaxSize())
                            Text("${(rate * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }
                        Column(Modifier.weight(1f)) { Text("$complete of ${tasks.size} completed", color = Color.White, style = MaterialTheme.typography.titleSmall); Text("Every step adds up", color = Color(0xFFC6D8DC), style = MaterialTheme.typography.bodySmall) }
                        Icon(Icons.Default.AutoAwesome, null, tint = HeroCoral, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Pending", "${pending.size}", Icons.Default.PendingActions, CobaltBlue, Modifier.weight(1f))
                StatTile("Overdue", "$overdue", Icons.Default.Schedule, Danger, Modifier.weight(1f))
                StatTile("Points", "${repo.getRewardPoints(user.userId)}", Icons.Default.EmojiEvents, WarningAmber, Modifier.weight(1f))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Your shortcuts")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard("Assignments", Icons.Default.Assignment, CobaltBlue, Modifier.weight(1f)) { navController.navigate(Screen.Assignments.route) }
                    QuickActionCard("Notes", Icons.Default.NoteAlt, PurpleAccent, Modifier.weight(1f)) { navController.navigate(Screen.Notes.route) }
                    QuickActionCard("GPA", Icons.Default.Calculate, WarningAmber, Modifier.weight(1f)) { navController.navigate(Screen.GPACalculator.route) }
                }
            }
        }
        item { SectionHeading("Your next steps", action = { TextButton(onClick = { navController.navigate(Screen.Assignments.route) }) { Text("View all") } }) }
        if (pending.isEmpty()) item { EmptyState(Icons.Default.TaskAlt, "You're all caught up", "Create an assignment to plan your next step.") }
        items(pending.take(3), key = { "task:${it.taskId}" }) { task ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = if (task.dueDate < today) HighPriorityBackground else LightBlue, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Assignment, null, tint = if (task.dueDate < today) Danger else CobaltBlue, modifier = Modifier.padding(12.dp).size(22.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleSmall, color = MainText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (task.dueDate < today) "Overdue · ${task.dueDate}" else "Due ${task.dueDate}", style = MaterialTheme.typography.bodySmall, color = if (task.dueDate < today) Danger else SecondaryText)
                    }
                    Checkbox(checked = false, onCheckedChange = { if (it) repo.updateTask(task.copy(status = TaskStatus.Completed)) })
                }
            }
        }
        if (announcements.isNotEmpty()) {
            item { SectionHeading("Around campus") }
            items(announcements, key = { "announcement:${it.announcementId}" }) { announcement ->
                Surface(color = LightBlue, shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Campaign, null, tint = CobaltBlue)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(announcement.title, style = MaterialTheme.typography.titleSmall, color = MainText); Text(announcement.message, style = MaterialTheme.typography.bodyMedium, color = SecondaryText) }
                    }
                }
            }
        }
    }
}
@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Surface(modifier, color = SurfaceWhite, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MainText)
            Text(label, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
    }
}
@Composable
private fun QuickActionCard(title: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick, modifier = modifier, color = tint.copy(alpha = 0.10f), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) { Icon(icon, null, tint = tint); Text(title, style = MaterialTheme.typography.labelMedium, color = MainText) }
    }
}

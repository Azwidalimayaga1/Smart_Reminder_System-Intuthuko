package com.intuthuko.smartreminder.ui.screens.notifications

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(user: User, navController: NavController) {
    val repository = remember { AppRepository() }
    var notifs by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getNotificationsForUser(user.userId)) }

    Scaffold(
        topBar = { AppHeader("Notifications", "Your latest updates.", Icons.Default.Notifications, onBack = { navController.popBackStack() }) { TextButton(onClick = { repository.markAllNotificationsRead(user.userId); notifs = repository.getNotificationsForUser(user.userId) }) { Text("Read all") } } }
    ) { padding ->
        val today = notifs.filter { it.timestamp == java.time.LocalDate.now().toString() }
        val earlier = notifs.filter { it.timestamp != java.time.LocalDate.now().toString() }

        if (notifs.isEmpty()) {
            EmptyState(icon = Icons.Default.NotificationsOff, title = "No notifications", subtitle = "You're all caught up!", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (today.isNotEmpty()) {
                    item { Text("Today", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecondaryText) }
                    items(today) { notif -> NotificationItem(notif, onRead = { repository.markNotificationRead(notif.notificationId); notifs = repository.getNotificationsForUser(user.userId) }) }
                }
                if (earlier.isNotEmpty()) {
                    item { Text("Earlier", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecondaryText, modifier = Modifier.padding(top = 8.dp)) }
                    items(earlier) { notif -> NotificationItem(notif, onRead = { repository.markNotificationRead(notif.notificationId); notifs = repository.getNotificationsForUser(user.userId) }) }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onRead: () -> Unit) {
    val icon = when (notification.category) {
        NotificationCategory.Assignment -> Icons.Default.Assignment
        NotificationCategory.Class -> Icons.Default.Event
        NotificationCategory.Exam -> Icons.Default.School
        NotificationCategory.Announcement -> Icons.Default.Campaign
        NotificationCategory.Subject -> Icons.Default.MenuBook
        NotificationCategory.General -> Icons.Default.Info
    }
    val iconColor = when (notification.category) {
        NotificationCategory.Exam -> Danger
        NotificationCategory.Announcement -> CobaltBlue
        NotificationCategory.Assignment -> WarningAmber
        else -> SecondaryText
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRead),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (notification.isRead) SurfaceWhite else CobaltBlue.copy(alpha = 0.04f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                Text(notification.message, fontSize = 13.sp, color = SecondaryText)
                Text(notification.timestamp, fontSize = 11.sp, color = Gray400)
            }
            if (!notification.isRead) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CobaltBlue))
            }
        }
    }
}

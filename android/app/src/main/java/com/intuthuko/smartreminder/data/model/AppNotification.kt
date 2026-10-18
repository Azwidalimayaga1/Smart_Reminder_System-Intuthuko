package com.intuthuko.smartreminder.data.model

data class Notification(
    val notificationId: Int,
    val userId: Int,
    val title: String,
    val message: String,
    val icon: String = "info",
    val timestamp: String = "",
    val isRead: Boolean = false,
    val screenRoute: String? = null,
    val category: NotificationCategory = NotificationCategory.General
)

enum class NotificationCategory(val displayName: String) {
    Assignment("Assignment"),
    Class("Class"),
    Exam("Exam"),
    Announcement("Announcement"),
    Subject("Subject"),
    General("General")
}

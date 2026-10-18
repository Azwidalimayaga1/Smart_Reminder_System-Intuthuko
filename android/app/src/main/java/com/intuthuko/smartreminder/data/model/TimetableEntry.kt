package com.intuthuko.smartreminder.data.model

data class TimetableEntry(
    val entryId: Int,
    val subjectName: String,
    val lecturer: String,
    val room: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val color: String = "#2F6BFF"
)

data class Announcement(
    val announcementId: Int,
    val title: String,
    val message: String,
    val timestamp: String = "",
    val isDismissed: Boolean = false
)

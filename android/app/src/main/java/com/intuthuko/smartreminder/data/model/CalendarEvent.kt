package com.intuthuko.smartreminder.data.model

enum class EventType(val displayName: String) {
    Meeting("Meeting"),
    Exam("Exam"),
    Workshop("Workshop"),
    Deadline("Deadline"),
    TaskDeadline("Task Deadline")
}

data class CalendarEvent(
    val eventId: Int,
    val userId: Int,
    val title: String,
    val description: String = "",
    val eventType: EventType = EventType.Meeting,
    val eventDate: String,
    val eventTime: String? = null,
    val endDate: String? = null,
    val endTime: String? = null,
    val color: String = "#1e40af"
)

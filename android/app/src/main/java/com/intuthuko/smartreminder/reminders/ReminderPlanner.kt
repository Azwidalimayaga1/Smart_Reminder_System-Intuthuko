package com.intuthuko.smartreminder.reminders

import com.intuthuko.smartreminder.data.model.*
import java.time.*

data class PlannedReminder(
    val key: String,
    val owner: String,
    val userId: Int,
    val title: String,
    val message: String,
    val atMillis: Long,
    val expiresMillis: Long,
    val localTime: String?,
    val localExpiry: String?,
    val category: String,
    val route: String
)

/** Pure planning logic, independent of Android and Firebase. */
object ReminderPlanner {
    fun plan(
        userId: Int, owner: String, tasks: List<Task>, events: List<CalendarEvent>,
        now: Instant, zone: ZoneId,
        enabled: Set<NotificationCategory> = NotificationCategory.entries.toSet(),
        taskKey: (Int) -> String = { it.toString() }, eventKey: (Int) -> String = { it.toString() }
    ): List<PlannedReminder> {
        val today = now.atZone(zone).toLocalDate()
        val result = mutableListOf<PlannedReminder>()
        if (NotificationCategory.Assignment in enabled) tasks.filter { it.userId == userId && it.status == TaskStatus.Pending }.forEach { task ->
            runCatching {
                val date = LocalDate.parse(task.dueDate)
                if (date < today) return@runCatching
                val time = date.atTime(9, 0)
                val expiry = date.plusDays(1).atStartOfDay()
                val scheduled = time.atZone(zone).toInstant().toEpochMilli().coerceAtLeast(now.toEpochMilli() + 5000)
                result += PlannedReminder("task:${taskKey(task.taskId)}:$date", owner, userId,
                    "Assignment due today", task.title.take(160), scheduled, expiry.atZone(zone).toInstant().toEpochMilli(),
                    time.toString(), expiry.toString(), NotificationCategory.Assignment.name, "assignments")
            }
        }
        events.filter { it.userId == userId }.forEach { event ->
            runCatching {
                val category = when (event.eventType) {
                    EventType.Exam -> NotificationCategory.Exam
                    EventType.Deadline, EventType.TaskDeadline -> NotificationCategory.Assignment
                    else -> NotificationCategory.Class
                }
                if (category !in enabled) return@runCatching
                val date = LocalDate.parse(event.eventDate)
                val time = if (event.eventTime.isNullOrBlank()) LocalTime.of(9, 0) else LocalTime.parse(event.eventTime)
                val starts = date.atTime(time)
                val expires = starts.plusMinutes(15)
                if (expires.atZone(zone).toInstant() <= now) return@runCatching
                val trigger = starts.minusMinutes(30)
                result += PlannedReminder("event:${eventKey(event.eventId)}:$starts", owner, userId,
                    "${event.eventType.displayName} reminder", "${event.title.take(160)} · ${time.toString().take(5)}",
                    trigger.atZone(zone).toInstant().toEpochMilli().coerceAtLeast(now.toEpochMilli() + 5000),
                    expires.atZone(zone).toInstant().toEpochMilli(), trigger.toString(), expires.toString(), category.name, "calendar")
            }
        }
        return result.sortedBy { it.atMillis }
    }
}

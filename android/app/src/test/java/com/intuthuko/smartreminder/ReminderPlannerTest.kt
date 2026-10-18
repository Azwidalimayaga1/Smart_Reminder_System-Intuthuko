package com.intuthuko.smartreminder

import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.reminders.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderPlannerTest {
    private val now = Instant.parse("2026-10-18T06:00:00Z")
    private val zone = ZoneId.of("Africa/Johannesburg")
    private fun task(id: Int = 1, user: Int = 6, date: String = "2026-10-19", status: TaskStatus = TaskStatus.Pending) = Task(id, user, user, "Submit report", dueDate = date, status = status)
    private fun event(id: Int = 1, user: Int = 6, date: String = "2026-10-18", time: String? = "10:00", type: EventType = EventType.Meeting) = CalendarEvent(id, user, "Study session", eventType = type, eventDate = date, eventTime = time)
    private fun plans(tasks: List<Task> = emptyList(), events: List<CalendarEvent> = emptyList(), at: Instant = now) = ReminderPlanner.plan(6, "firebase:owner", tasks, events, at, zone)

    @Test fun assignmentUsesNineInThePhonesTimeZone() {
        val reminder = plans(tasks = listOf(task())).single()
        assertEquals(Instant.parse("2026-10-19T07:00:00Z").toEpochMilli(), reminder.atMillis)
        assertEquals("assignments", reminder.route)
    }
    @Test fun completedSubmittedAndOtherPersonsTasksAreExcluded() {
        assertTrue(plans(tasks = listOf(task(status = TaskStatus.Completed), task(2, status = TaskStatus.Submitted), task(3, user = 8))).isEmpty())
    }
    @Test fun oldAssignmentsDoNotProduceStaleAlerts() {
        assertTrue(plans(tasks = listOf(task(date = "2026-10-17"))).isEmpty())
    }
    @Test fun assignmentCreatedAfterNineGetsOneCatchUpOccurrence() {
        val later = Instant.parse("2026-10-18T12:00:00Z")
        val first = plans(tasks = listOf(task(date = "2026-10-18")), at = later).single()
        val second = plans(tasks = listOf(task(date = "2026-10-18")), at = later.plusSeconds(10)).single()
        assertEquals(later.plusSeconds(5).toEpochMilli(), first.atMillis)
        assertEquals(first.key, second.key) // The delivered-occurrence ledger prevents a repeat on every edit.
    }
    @Test fun eventAlertsThirtyMinutesBeforeStart() {
        val reminder = plans(events = listOf(event())).single()
        assertEquals(Instant.parse("2026-10-18T07:30:00Z").toEpochMilli(), reminder.atMillis)
        assertEquals("calendar", reminder.route)
    }
    @Test fun untimedEventUsesNineInTheMorning() {
        val reminder = plans(events = listOf(event(time = null))).single()
        assertEquals(Instant.parse("2026-10-18T06:30:00Z").toEpochMilli(), reminder.atMillis)
    }
    @Test fun upcomingEventInsideTheLeadWindowGetsCatchUpAlert() {
        val reminder = plans(events = listOf(event(time = "08:10"))).single()
        assertEquals(now.plusSeconds(5).toEpochMilli(), reminder.atMillis)
    }
    @Test fun expiredAndOtherPersonsEventsAreExcluded() {
        assertTrue(plans(events = listOf(event(time = "07:00"), event(2, user = 8))).isEmpty())
    }
    @Test fun malformedDatesAndTimesCannotCrashScheduling() {
        assertTrue(plans(tasks = listOf(task(date = "2026-99-99")), events = listOf(event(time = "tomorrow"), event(2, date = "bad"))).isEmpty())
    }
    @Test fun disabledCategoriesAreHonoured() {
        val reminders = ReminderPlanner.plan(6, "firebase:owner", listOf(task()), listOf(event(type = EventType.Exam), event(2)), now, zone, setOf(NotificationCategory.Class))
        assertEquals(1, reminders.size)
        assertEquals("Class", reminders.single().category)
    }
    @Test fun taskAndEventWithSameNumericIdentifierHaveDistinctOccurrences() {
        val reminders = plans(tasks = listOf(task()), events = listOf(event()))
        assertEquals(2, reminders.map { it.key }.toSet().size)
    }
    @Test fun cloudDocumentIdentitySurvivesLocalIdentifierRemapping() {
        val first = ReminderPlanner.plan(6, "firebase:owner", listOf(task(100)), emptyList(), now, zone, taskKey = { "stable-cloud-id" }).single()
        val next = ReminderPlanner.plan(6, "firebase:owner", listOf(task(999)), emptyList(), now, zone, taskKey = { "stable-cloud-id" }).single()
        assertEquals(first.key, next.key)
    }
    @Test fun changingDeadlineCreatesANewOccurrenceAndDeletingItRemovesThePlan() {
        val original = plans(tasks = listOf(task())).single()
        val changed = plans(tasks = listOf(task(date = "2026-10-20"))).single()
        assertNotEquals(original.key, changed.key)
        assertTrue(plans().isEmpty())
    }
    @Test fun timeZoneChangesKeepTheReminderAtLocalNine() {
        val johannesburg = plans(tasks = listOf(task())).single()
        val utc = ReminderPlanner.plan(6, "firebase:owner", listOf(task()), emptyList(), now, ZoneOffset.UTC).single()
        assertEquals(Duration.ofHours(2).toMillis(), utc.atMillis - johannesburg.atMillis)
    }
}

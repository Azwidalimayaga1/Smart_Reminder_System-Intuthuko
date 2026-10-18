package com.intuthuko.smartreminder.data.repository

import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.CloudStore
import com.intuthuko.smartreminder.data.model.*

class AppRepository {

    fun authenticate(email: String, password: String, role: UserRole): User? {
        return MockData.users.find { it.email.equals(email, ignoreCase = true) && com.intuthuko.smartreminder.data.Passwords.verify(password, it.password) && it.role == role }
    }

    fun register(username: String, email: String, password: String, role: UserRole, studentNumber: String = ""): User? {
        if (MockData.users.any { it.email.equals(email.trim(), true) }) return null
        if (MockData.users.any { it.username == username }) return null
        return MockData.addUser(User(0, username.trim(), email.trim(), com.intuthuko.smartreminder.data.Passwords.hash(password), role, studentNumber = studentNumber))
    }

    fun getUser(userId: Int): User? = MockData.users.find { it.userId == userId }
    fun getAllUsers(): List<User> = if (CloudStore.active) MockData.users.filter { it.userId == CloudStore.userId } else MockData.users.toList()
    fun updateUser(user: User) { val index = MockData.users.indexOfFirst { it.userId == user.userId }; if (index >= 0) { MockData.users[index] = user; com.intuthuko.smartreminder.data.LocalStore.save() } }

    fun getTasksForUser(userId: Int): List<Task> = MockData.tasks.filter { it.userId == userId || it.assignedBy == userId }
    fun getPendingTasks(userId: Int): List<Task> = MockData.tasks.filter { it.userId == userId && it.status == TaskStatus.Pending }
    fun getCompletedTasks(userId: Int): List<Task> = MockData.tasks.filter { it.userId == userId && it.status == TaskStatus.Completed }
    fun createTask(task: Task): Task = MockData.addTask(task)
    fun updateTask(task: Task) = MockData.updateTask(task)
    fun deleteTask(taskId: Int) = MockData.deleteTask(taskId)

    fun getEnrolledSubjects(userId: Int): List<Subject> = MockData.userSubjects.filter { it.userId == userId }.mapNotNull { us -> MockData.subjects.find { it.subjectId == us.subjectId } ?: if (CloudStore.active) CloudStore.enrolledSubject(us.subjectId) else null }
    fun getAvailableSubjects(userId: Int): List<Subject> { val enrolled = MockData.userSubjects.filter { it.userId == userId }.map { it.subjectId }; return MockData.subjects.filter { it.subjectId !in enrolled } }
    fun getAllSubjects(): List<Subject> = MockData.subjects.toList()
    fun enrollSubject(userId: Int, subjectId: Int) = MockData.enrollSubject(userId, subjectId)
    fun dropSubject(userId: Int, subjectId: Int) = MockData.dropSubject(userId, subjectId)
    fun addSubject(subject: Subject): Subject { if (CloudStore.active) { CloudStore.addSubject(subject); return subject }; return MockData.addSubject(subject) }
    fun deleteSubject(subjectId: Int) { if (CloudStore.active) CloudStore.deleteSubject(subjectId) else MockData.deleteSubject(subjectId) }

    fun getEventsForUser(userId: Int): List<CalendarEvent> = MockData.calendarEvents.filter { it.userId == userId }
    fun getAllEvents(): List<CalendarEvent> = MockData.calendarEvents.toList()
    fun addEvent(event: CalendarEvent): CalendarEvent = MockData.addEvent(event)
    fun deleteEvent(eventId: Int) = MockData.deleteEvent(eventId)

    fun getNotesForUser(userId: Int): List<Note> = MockData.notes.filter { it.userId == userId }
    fun addNote(note: Note): Note = MockData.addNote(note)
    fun updateNote(note: Note) = MockData.updateNote(note)
    fun deleteNote(noteId: Int) = MockData.deleteNote(noteId)

    fun getTimetableEntries(): List<TimetableEntry> = MockData.timetableEntries.toList()
    fun getTimetableForDay(dayOfWeek: Int): List<TimetableEntry> = MockData.timetableEntries.filter { it.dayOfWeek == dayOfWeek }

    fun getAnnouncements(): List<Announcement> = MockData.announcements.filter { !it.isDismissed && (!CloudStore.active || !com.intuthuko.smartreminder.data.LocalStore.prefs.getBoolean("dismissed_${CloudStore.userId}_${it.announcementId}", false)) }
    fun dismissAnnouncement(id: Int) { if (CloudStore.active) { com.intuthuko.smartreminder.data.LocalStore.prefs.edit().putBoolean("dismissed_${CloudStore.userId}_$id", true).apply(); com.intuthuko.smartreminder.data.LocalStore.changed() } else MockData.dismissAnnouncement(id) }

    fun getNotificationsForUser(userId: Int): List<Notification> = MockData.notifications.filter { it.userId == userId && com.intuthuko.smartreminder.data.LocalStore.prefs.getBoolean("notification_${userId}_${it.category.name}", true) }
    fun getUnreadNotificationCount(userId: Int): Int = getNotificationsForUser(userId).count { !it.isRead }
    fun markNotificationRead(id: Int) = MockData.markNotificationRead(id)
    fun markAllNotificationsRead(userId: Int) = MockData.markAllNotificationsRead(userId)
    fun deleteNotification(id: Int) = MockData.deleteNotification(id)

    fun getGPAModules(): List<GPAModule> = MockData.gpaModules.toList()
    fun addGPAModule(module: GPAModule): GPAModule = MockData.addGPAModule(module)
    fun updateGPAModule(module: GPAModule) = MockData.updateGPAModule(module)
    fun deleteGPAModule(moduleId: Int) = MockData.deleteGPAModule(moduleId)
    fun resetGPAModules() = MockData.resetGPAModules()

    fun calculateGPA(): Double {
        val modules = MockData.gpaModules
        if (modules.isEmpty()) return 0.0
        val totalCredits = modules.sumOf { it.credits }
        if (totalCredits == 0) return 0.0
        val weightedSum = modules.sumOf { module ->
            val gradePoint = when {
                module.mark >= 80 -> 4.0
                module.mark >= 70 -> 3.0
                module.mark >= 60 -> 2.0
                module.mark >= 50 -> 1.0
                else -> 0.0
            }
            gradePoint * module.credits
        }
        return Math.round((weightedSum / totalCredits) * 100.0) / 100.0
    }

    fun getCompletionRate(userId: Int): Double {
        val total = MockData.tasks.count { it.userId == userId }
        if (total == 0) return 0.0
        val completed = MockData.tasks.count { it.userId == userId && it.status == TaskStatus.Completed }
        return (completed.toDouble() / total) * 100
    }

    fun getRewardPoints(userId: Int): Int {
        val completed = MockData.tasks.count { it.userId == userId && it.status == TaskStatus.Completed }
        return completed * 10
    }

    fun getStudentCount(): Int = MockData.users.count { it.role == UserRole.Student }
    fun getTaskCount(): Int = MockData.tasks.size
    fun getCompletedTaskCount(): Int = MockData.tasks.count { it.status == TaskStatus.Completed }
    fun deleteUser(userId: Int) { if (!CloudStore.active) MockData.deleteUser(userId) }
}


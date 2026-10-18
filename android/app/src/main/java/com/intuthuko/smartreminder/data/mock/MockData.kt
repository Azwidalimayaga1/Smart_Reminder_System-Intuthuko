package com.intuthuko.smartreminder.data.mock

import com.intuthuko.smartreminder.data.model.*

// Fictional local development fixtures only. No real people or contact details.
// Reserved example.invalid addresses cannot deliver email. These are not Firebase accounts.
object MockData {

    val users = mutableListOf(
        User(1, "Demo Student One", "student1@example.invalid", "pass123", UserRole.Student, "2024-01-15", course = "Bachelor of Science", yearOfStudy = 2),
        User(2, "Demo Student Two", "student2@example.invalid", "pass123", UserRole.Student, "2024-02-01", course = "Bachelor of Commerce", yearOfStudy = 1),
        User(3, "Demo Tutor One", "tutor1@example.invalid", "pass123", UserRole.Tutor, "2024-01-10"),
        User(4, "Demo Tutor Two", "tutor2@example.invalid", "pass123", UserRole.Tutor, "2024-01-10"),
        User(5, "Admin User", "admin@example.invalid", "pass123", UserRole.Admin, "2024-01-01")
    )

    val subjects = mutableListOf(
        Subject(1, "Mathematics", 250.00),
        Subject(2, "Physical Sciences", 250.00),
        Subject(3, "Life Sciences", 220.00),
        Subject(4, "English", 200.00),
        Subject(5, "Accounting", 230.00),
        Subject(6, "Business Studies", 220.00)
    )

    val userSubjects = mutableListOf(
        UserSubject(1, 1, "2024-01-15"),
        UserSubject(1, 2, "2024-01-15"),
        UserSubject(1, 4, "2024-01-16"),
        UserSubject(2, 1, "2024-02-01"),
        UserSubject(2, 3, "2024-02-01"),
        UserSubject(2, 5, "2024-02-02")
    )

    val tasks = mutableListOf(
        Task(1, 1, 3, "Complete Algebra Worksheet", "Solve equations 1-20 from chapter 3", "2026-09-06", TaskPriority.High, TaskStatus.Pending),
        Task(2, 1, 3, "Physics Lab Report", "Write up the momentum experiment results", "2026-09-10", TaskPriority.Medium, TaskStatus.Pending),
        Task(3, 1, 1, "Review Biology Notes", "Go through chapter 5 cell division", "2026-09-08", TaskPriority.Low, TaskStatus.Completed, isSelfCreated = true),
        Task(4, 2, 4, "Business Plan Draft", "Prepare initial draft for entrepreneurship project", "2026-09-05", TaskPriority.High, TaskStatus.Pending),
        Task(5, 2, 4, "Accounting Practice Questions", "Complete exercise set on balance sheets", "2026-09-12", TaskPriority.Medium, TaskStatus.Submitted, "Here is my submission for the accounting questions.", submittedAt = "2026-09-03"),
        Task(6, 1, 1, "Study for English Test", "Prepare for Friday's comprehension test", "2026-09-05", TaskPriority.High, TaskStatus.Pending, isSelfCreated = true),
        Task(7, 2, 2, "Math Olympiad Preparation", "Work through past competition papers", "2026-09-15", TaskPriority.Medium, TaskStatus.Pending),
        Task(8, 1, 3, "History Essay", "Write a 500-word essay on the Renaissance", "2026-09-20", TaskPriority.Low, TaskStatus.Pending)
    )

    val calendarEvents = mutableListOf(
        CalendarEvent(1, 1, "Math Tutoring Session", "Extra class with Demo Tutor One", EventType.Meeting, "2026-09-05", "14:00", "2026-09-05", "16:00"),
        CalendarEvent(2, 1, "Physical Sciences Exam", "Chapter 4-6 written test", EventType.Exam, "2026-09-10", "09:00", "2026-09-10", "11:00"),
        CalendarEvent(3, 2, "Career Workshop", "CV writing and interview skills", EventType.Workshop, "2026-09-08", "10:00", "2026-09-08", "12:00"),
        CalendarEvent(4, 1, "Project Deadline", "Submit science fair proposal", EventType.Deadline, "2026-09-12", null, null, null),
        CalendarEvent(5, 2, "English Literature Class", "Discuss To Kill a Mockingbird", EventType.Meeting, "2026-09-06", "13:00", "2026-09-06", "14:30"),
        CalendarEvent(6, 1, "Business Studies Workshop", "Group presentation practice", EventType.Workshop, "2026-09-14", "09:00", "2026-09-14", "11:00")
    )

    val notes = mutableListOf(
        Note(1, 1, "Quadratic Equations", "The quadratic formula: x = (-b ± √(b²-4ac)) / 2a\n\nDiscriminant determines the nature of roots:\n- b²-4ac > 0: two real roots\n- b²-4ac = 0: one repeated root\n- b²-4ac < 0: no real roots", "Mathematics", isPinned = true, updatedAt = "2026-09-03", isOfflineAvailable = true),
        Note(2, 1, "Momentum Experiment", "Conservation of momentum: m₁v₁ + m₂v₂ = m₁v₁' + m₂v₂'\n\nIn an elastic collision, both momentum and kinetic energy are conserved.\n\nIn an inelastic collision, only momentum is conserved.", "Physical Sciences", isPinned = false, updatedAt = "2026-09-02", isOfflineAvailable = true),
        Note(3, 1, "Poetry Devices", "Key poetic devices to remember:\n- Metaphor: direct comparison\n- Simile: comparison using like/as\n- Personification: human qualities to objects\n- Alliteration: repetition of initial sounds\n- Imagery: sensory language\n- Onomatopoeia: sound words", "English", isPinned = true, updatedAt = "2026-09-01"),
        Note(4, 1, "Cell Division Notes", "Mitosis stages: Prophase, Metaphase, Anaphase, Telophase\n\nMeiosis produces 4 haploid cells vs mitosis producing 2 diploid cells.", "Life Sciences", isPinned = false, updatedAt = "2026-08-30")
    )

    val timetableEntries = mutableListOf(
        TimetableEntry(1, "Mathematics", "Demo Tutor Two", "Room 101", 1, "08:00", "09:30", "#2F6BFF"),
        TimetableEntry(2, "Physical Sciences", "Demo Tutor Three", "Room 203", 1, "10:00", "11:30", "#EF5B62"),
        TimetableEntry(3, "English", "Demo Tutor One", "Room 105", 1, "13:00", "14:30", "#17B77E"),
        TimetableEntry(4, "Mathematics", "Demo Tutor Two", "Room 101", 2, "08:00", "09:30", "#2F6BFF"),
        TimetableEntry(5, "Life Sciences", "Demo Tutor Four", "Lab 3", 2, "10:00", "11:30", "#F2A93B"),
        TimetableEntry(6, "Accounting", "Demo Tutor Five", "Room 305", 2, "13:00", "14:30", "#7B61E8"),
        TimetableEntry(7, "Physical Sciences", "Demo Tutor Three", "Room 203", 3, "08:00", "09:30", "#EF5B62"),
        TimetableEntry(8, "English", "Demo Tutor One", "Room 105", 3, "10:00", "11:30", "#17B77E"),
        TimetableEntry(9, "Mathematics", "Demo Tutor Two", "Room 101", 4, "08:00", "09:30", "#2F6BFF"),
        TimetableEntry(10, "Business Studies", "Demo Tutor Six", "Room 402", 4, "10:00", "11:30", "#F2A93B"),
        TimetableEntry(11, "Life Sciences", "Demo Tutor Four", "Lab 3", 4, "13:00", "14:30", "#17B77E"),
        TimetableEntry(12, "Physical Sciences", "Demo Tutor Three", "Room 203", 5, "08:00", "09:30", "#EF5B62"),
        TimetableEntry(13, "Mathematics", "Demo Tutor Two", "Room 101", 5, "10:00", "11:30", "#2F6BFF"),
        TimetableEntry(14, "English", "Demo Tutor One", "Room 105", 5, "13:00", "14:30", "#17B77E")
    )

    val announcements = mutableListOf(
        Announcement(1, "Campus Announcement", "Library extended hours this week. Open until 20:00.", "2026-09-03"),
        Announcement(2, "Sports Day", "Annual sports day will be held on 15 September. All students welcome.", "2026-09-01")
    )

    val notifications = mutableListOf(
        Notification(1, 1, "Assignment Due", "Complete Algebra Worksheet is due tomorrow", "assignment", "2026-09-04", false, "assignments", NotificationCategory.Assignment),
        Notification(2, 1, "Exam Reminder", "Physical Sciences Exam in 6 days", "exam", "2026-09-04", false, "calendar", NotificationCategory.Exam),
        Notification(3, 1, "New Announcement", "Library extended hours this week", "announcement", "2026-09-03", true, null, NotificationCategory.Announcement),
        Notification(4, 1, "Task Assigned", "Demo Tutor One assigned you 'Physics Lab Report'", "assignment", "2026-09-02", true, "assignments", NotificationCategory.Assignment),
        Notification(5, 1, "Subject Update", "New timetable available for Mathematics", "subject", "2026-09-01", true, null, NotificationCategory.Subject)
    )

    val gpaModules = mutableListOf(
        GPAModule(1, "Mathematics", 16, 78),
        GPAModule(2, "Physical Sciences", 16, 72),
        GPAModule(3, "English", 12, 81),
        GPAModule(4, "Life Sciences", 14, 75),
        GPAModule(5, "Accounting", 12, 68)
    )

    private var nextUserId = 6
    private var nextTaskId = 9
    private var nextEventId = 7
    private var nextSubjectId = 7
    private var nextNoteId = 5
    private var nextNotificationId = 6
    private var nextAnnouncementId = 3
    private var nextGpaModuleId = 6

    fun addUser(user: User): User { val newUser = user.copy(userId = nextUserId++); users.add(newUser); com.intuthuko.smartreminder.data.LocalStore.save(); return newUser }
    fun addTask(task: Task): Task { val newTask = task.copy(taskId = nextTaskId++); tasks.add(newTask); com.intuthuko.smartreminder.data.LocalStore.save(); return newTask }
    fun updateTask(task: Task) { val index = tasks.indexOfFirst { it.taskId == task.taskId }; if (index >= 0) tasks[index] = task ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun deleteTask(taskId: Int) { tasks.removeAll { it.taskId == taskId } ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun addEvent(event: CalendarEvent): CalendarEvent { val newEvent = event.copy(eventId = nextEventId++); calendarEvents.add(newEvent); com.intuthuko.smartreminder.data.LocalStore.save(); return newEvent }
    fun deleteEvent(eventId: Int) { calendarEvents.removeAll { it.eventId == eventId } ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun addSubject(subject: Subject): Subject { val newSubject = subject.copy(subjectId = nextSubjectId++); subjects.add(newSubject); com.intuthuko.smartreminder.data.LocalStore.save(); return newSubject }
    fun deleteSubject(subjectId: Int) { subjects.removeAll { it.subjectId == subjectId }; userSubjects.removeAll { it.subjectId == subjectId } ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun enrollSubject(userId: Int, subjectId: Int) { if (userSubjects.none { it.userId == userId && it.subjectId == subjectId }) userSubjects.add(UserSubject(userId, subjectId)) ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun dropSubject(userId: Int, subjectId: Int) { userSubjects.removeAll { it.userId == userId && it.subjectId == subjectId } ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun deleteUser(userId: Int) { users.removeAll { it.userId == userId }; tasks.removeAll { it.userId == userId || it.assignedBy == userId }; calendarEvents.removeAll { it.userId == userId }; userSubjects.removeAll { it.userId == userId }; notes.removeAll { it.userId == userId }; notifications.removeAll { it.userId == userId } ; com.intuthuko.smartreminder.data.LocalStore.save() }

    fun addNote(note: Note): Note { val newNote = note.copy(noteId = nextNoteId++); notes.add(newNote); com.intuthuko.smartreminder.data.LocalStore.save(); return newNote }
    fun updateNote(note: Note) { val index = notes.indexOfFirst { it.noteId == note.noteId }; if (index >= 0) notes[index] = note ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun deleteNote(noteId: Int) { notes.removeAll { it.noteId == noteId } ; com.intuthuko.smartreminder.data.LocalStore.save() }

    fun addNotification(notification: Notification): Notification { val newNotification = notification.copy(notificationId = nextNotificationId++); notifications.add(newNotification); com.intuthuko.smartreminder.data.LocalStore.save(); return newNotification }
    fun markNotificationRead(notificationId: Int) { val index = notifications.indexOfFirst { it.notificationId == notificationId }; if (index >= 0) notifications[index] = notifications[index].copy(isRead = true) ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun markAllNotificationsRead(userId: Int) { notifications.forEachIndexed { i, n -> if (n.userId == userId && !n.isRead) notifications[i] = n.copy(isRead = true) } ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun deleteNotification(notificationId: Int) { notifications.removeAll { it.notificationId == notificationId } ; com.intuthuko.smartreminder.data.LocalStore.save() }

    fun dismissAnnouncement(announcementId: Int) { val index = announcements.indexOfFirst { it.announcementId == announcementId }; if (index >= 0) announcements[index] = announcements[index].copy(isDismissed = true) ; com.intuthuko.smartreminder.data.LocalStore.save() }

    fun addGPAModule(module: GPAModule): GPAModule { val newModule = module.copy(moduleId = nextGpaModuleId++); gpaModules.add(newModule); com.intuthuko.smartreminder.data.LocalStore.save(); return newModule }
    fun updateGPAModule(module: GPAModule) { val index = gpaModules.indexOfFirst { it.moduleId == module.moduleId }; if (index >= 0) gpaModules[index] = module ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun deleteGPAModule(moduleId: Int) { gpaModules.removeAll { it.moduleId == moduleId } ; com.intuthuko.smartreminder.data.LocalStore.save() }
    fun resetGPAModules() { gpaModules.clear() ; com.intuthuko.smartreminder.data.LocalStore.save() }
}

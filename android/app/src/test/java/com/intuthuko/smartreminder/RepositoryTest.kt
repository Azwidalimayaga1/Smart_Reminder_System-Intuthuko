package com.intuthuko.smartreminder

import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RepositoryTest {
    @Test fun registrationRejectsDuplicateEmailAndChecksPasswordAndRole() {
        val repository = AppRepository()
        val user = repository.register("Button Test", "button-test@example.invalid", "Password123!", UserRole.Student)!!
        try {
            assertNotEquals("Password123!", user.password)
            assertNotNull(repository.authenticate("BUTTON-TEST@example.invalid", "Password123!", UserRole.Student))
            assertNull(repository.authenticate(user.email, "wrong", UserRole.Student))
            assertNull(repository.authenticate(user.email, "Password123!", UserRole.Admin))
            assertNull(repository.register("Another name", "BUTTON-TEST@example.invalid", "Password123!", UserRole.Student))
        } finally { repository.deleteUser(user.userId) }
    }

    @Test fun assignmentEditCompletionAndDeleteAffectStoredRecords() {
        val repository = AppRepository()
        val created = repository.createTask(Task(0, 8888, 8888, "Before", "", "2026-10-25", TaskPriority.Low, TaskStatus.Pending))
        try {
            repository.updateTask(created.copy(title = "After", status = TaskStatus.Completed))
            val saved = repository.getTasksForUser(8888).single()
            assertEquals("After", saved.title)
            assertEquals(TaskStatus.Completed, saved.status)
            assertEquals(100.0, repository.getCompletionRate(8888), 0.01)
            repository.deleteTask(created.taskId)
            assertTrue(repository.getTasksForUser(8888).isEmpty())
        } finally { repository.deleteTask(created.taskId) }
    }
}

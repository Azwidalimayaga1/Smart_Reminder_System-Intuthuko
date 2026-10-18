package com.intuthuko.smartreminder.data.model

enum class TaskPriority(val displayName: String) {
    High("High"),
    Medium("Medium"),
    Low("Low")
}

enum class TaskStatus(val displayName: String) {
    Pending("Pending"),
    Submitted("Submitted"),
    Completed("Completed")
}

data class Task(
    val taskId: Int,
    val userId: Int,
    val assignedBy: Int,
    val title: String,
    val description: String = "",
    val dueDate: String,
    val priority: TaskPriority = TaskPriority.Medium,
    val status: TaskStatus = TaskStatus.Pending,
    val submissionNote: String? = null,
    val submissionPdfName: String? = null,
    val submittedAt: String? = null,
    val createdAt: String = "",
    val isSelfCreated: Boolean = false
)

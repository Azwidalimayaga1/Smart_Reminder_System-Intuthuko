package com.intuthuko.smartreminder.data.model

data class Note(
    val noteId: Int,
    val userId: Int,
    val title: String,
    val body: String = "",
    val subjectName: String = "",
    val isPinned: Boolean = false,
    val updatedAt: String = "",
    val isOfflineAvailable: Boolean = false
)

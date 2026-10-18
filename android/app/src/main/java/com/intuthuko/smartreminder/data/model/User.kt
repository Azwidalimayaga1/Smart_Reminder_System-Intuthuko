package com.intuthuko.smartreminder.data.model

enum class UserRole(val displayName: String) {
    Student("Student"),
    Tutor("Tutor"),
    Admin("Admin")
}

data class User(
    val userId: Int,
    val username: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val createdAt: String = "",
    val studentNumber: String = "",
    val phone: String = "",
    val institution: String = "Intuthuko College",
    val course: String = "",
    val yearOfStudy: Int = 1,
    val authUid: String = ""
)

package com.intuthuko.smartreminder.data.model

data class Subject(
    val subjectId: Int,
    val subjectName: String,
    val monthlyFee: Double
)

data class UserSubject(
    val userId: Int,
    val subjectId: Int,
    val selectedAt: String = ""
)

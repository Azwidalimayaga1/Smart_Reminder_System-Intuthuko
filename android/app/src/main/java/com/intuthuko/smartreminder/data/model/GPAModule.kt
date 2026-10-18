package com.intuthuko.smartreminder.data.model

data class GPAModule(
    val moduleId: Int,
    val moduleName: String,
    val credits: Int,
    val mark: Int
)

enum class Semester(val displayName: String) {
    Semester1("Semester 1"),
    Semester2("Semester 2"),
    YearEnd("Year End")
}

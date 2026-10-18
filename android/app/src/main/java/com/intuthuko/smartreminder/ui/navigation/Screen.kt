package com.intuthuko.smartreminder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Assignments : Screen("assignments")
    object Calendar : Screen("calendar")
    object Notes : Screen("notes")
    object More : Screen("more")
    object Profile : Screen("profile")
    object Subjects : Screen("subjects")
    object GPACalculator : Screen("gpa_calculator")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object Updates : Screen("updates")
    object Admin : Screen("admin")
}

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

val studentNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Screen.Home),
    NavItem("Tasks", Icons.Default.Assignment, Screen.Assignments),
    NavItem("Calendar", Icons.Default.CalendarMonth, Screen.Calendar),
    NavItem("Notes", Icons.Default.NoteAlt, Screen.Notes),
    NavItem("More", Icons.Default.MoreHoriz, Screen.More)
)

fun navItemsForRole(role: com.intuthuko.smartreminder.data.model.UserRole): List<NavItem> = when (role) {
    com.intuthuko.smartreminder.data.model.UserRole.Student -> studentNavItems
    com.intuthuko.smartreminder.data.model.UserRole.Tutor -> listOf(
        NavItem("Workspace", Icons.Default.SpaceDashboard, Screen.Home),
        NavItem("Planner", Icons.Default.CalendarMonth, Screen.Calendar),
        NavItem("Updates", Icons.Default.Campaign, Screen.Updates),
        NavItem("Notes", Icons.Default.NoteAlt, Screen.Notes),
        NavItem("More", Icons.Default.MoreHoriz, Screen.More)
    )
    com.intuthuko.smartreminder.data.model.UserRole.Admin -> listOf(
        NavItem("Overview", Icons.Default.Dashboard, Screen.Home),
        NavItem("Manage", Icons.Default.AdminPanelSettings, Screen.Admin),
        NavItem("Updates", Icons.Default.Campaign, Screen.Updates),
        NavItem("More", Icons.Default.MoreHoriz, Screen.More)
    )
}

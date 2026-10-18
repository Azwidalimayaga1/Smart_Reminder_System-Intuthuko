package com.intuthuko.smartreminder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.intuthuko.smartreminder.data.model.User
import com.intuthuko.smartreminder.ui.screens.login.LoginScreen
import com.intuthuko.smartreminder.ui.screens.home.HomeScreen
import com.intuthuko.smartreminder.ui.screens.assignments.AssignmentsScreen
import com.intuthuko.smartreminder.ui.screens.calendar.CalendarScreen
import com.intuthuko.smartreminder.ui.screens.notes.NotesScreen
import com.intuthuko.smartreminder.ui.screens.more.MoreScreen
import com.intuthuko.smartreminder.ui.screens.profile.ProfileScreen
import com.intuthuko.smartreminder.ui.screens.subjects.SubjectsScreen
import com.intuthuko.smartreminder.ui.screens.gpa.GPACalculatorScreen
import com.intuthuko.smartreminder.ui.screens.settings.SettingsScreen
import com.intuthuko.smartreminder.ui.screens.notifications.NotificationsScreen
import com.intuthuko.smartreminder.ui.screens.admin.AdminScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    currentUser: User?,
    onLogin: (User) -> Unit,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) Screen.Home.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = { user ->
                    onLogin(user)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            currentUser?.let { user ->
                HomeScreen(
                    user = user,
                    navController = navController
                )
            }
        }
        composable(Screen.Assignments.route) {
            currentUser?.let { user ->
                AssignmentsScreen(user = user)
            }
        }
        composable(Screen.Calendar.route) {
            currentUser?.let { user ->
                CalendarScreen(user = user)
            }
        }
        composable(Screen.Notes.route) {
            currentUser?.let { user ->
                NotesScreen(user = user, navController = navController)
            }
        }
        composable(Screen.More.route) {
            currentUser?.let { user ->
                MoreScreen(
                    user = com.intuthuko.smartreminder.data.repository.AppRepository().getUser(user.userId) ?: user,
                    navController = navController,
                    onLogout = onLogout
                )
            }
        }
        composable(Screen.Profile.route) {
            currentUser?.let { user ->
                ProfileScreen(user = com.intuthuko.smartreminder.data.repository.AppRepository().getUser(user.userId) ?: user, navController = navController)
            }
        }
        composable(Screen.Subjects.route) {
            currentUser?.let { user ->
                SubjectsScreen(user = user, onBack = { navController.popBackStack() })
            }
        }
        composable(Screen.GPACalculator.route) {
            GPACalculatorScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            currentUser?.let { SettingsScreen(navController = navController, user = it) }
        }
        composable(Screen.Notifications.route) {
            currentUser?.let { user ->
                NotificationsScreen(user = user, navController = navController)
            }
        }
        composable(Screen.Updates.route) {
            currentUser?.let { user ->
                com.intuthuko.smartreminder.ui.screens.home.CampusUpdatesScreen(user)
            }
        }
        composable(Screen.Admin.route) {
            currentUser?.let { user ->
                if (user.role == com.intuthuko.smartreminder.data.model.UserRole.Admin) AdminScreen(user = user, onBack = { navController.popBackStack() })
            }
        }
    }
}

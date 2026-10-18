package com.intuthuko.smartreminder

import android.os.Bundle
import android.content.Intent
import com.intuthuko.smartreminder.reminders.ReminderScheduler
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.intuthuko.smartreminder.data.model.User
import com.intuthuko.smartreminder.ui.navigation.*
import com.intuthuko.smartreminder.ui.theme.*

class MainActivity : ComponentActivity() {
    private var reminderLink by mutableStateOf<Pair<String, String>?>(null)
    private fun readReminderIntent(intent: Intent?) {
        if (intent?.action == ReminderScheduler.OPEN) {
            val owner = intent.getStringExtra("reminder_owner") ?: return
            val route = intent.getStringExtra("reminder_route") ?: return
            if (route in setOf("home", "assignments", "calendar")) reminderLink = owner to route
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); readReminderIntent(intent)
    }
    override fun onResume() {
        super.onResume(); ReminderScheduler.restore(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.initialize(this)
        readReminderIntent(intent)
        com.intuthuko.smartreminder.data.LocalStore.initialize(this)
        setContent {
            CampusSyncTheme {
                val sessionScope = rememberCoroutineScope()
                var currentUserId by rememberSaveable { mutableStateOf<Int?>(null) }
                val revision = com.intuthuko.smartreminder.data.LocalStore.revision
                val currentUser = currentUserId?.let { com.intuthuko.smartreminder.data.repository.AppRepository().getUser(it) }?.takeIf { it.authUid.isEmpty() || com.intuthuko.smartreminder.data.CloudStore.active }

                if (currentUser == null) {
                    LoginNavHost(
                        onLogin = { ReminderScheduler.activate(it); currentUserId = it.userId }
                    )
                } else {
                    key(currentUser!!.userId, currentUser!!.role) { MainApp(
                        user = currentUser!!,
                        reminderRoute = reminderLink?.takeIf { it.first == ReminderScheduler.identity(currentUser!!) }?.second,
                        onReminderConsumed = { reminderLink = null; intent.removeExtra("reminder_owner"); intent.removeExtra("reminder_route") },
                        onLogout = {
                            com.intuthuko.smartreminder.data.AuthService.signOut()
                            currentUserId = null
                            sessionScope.launch { com.intuthuko.smartreminder.data.AuthService.clearGoogleSession(this@MainActivity) }
                        }
                    ) }
                }
            }
        }
    }
}

@Composable
fun LoginNavHost(onLogin: (User) -> Unit) {
    val navController = rememberNavController()
    NavGraph(
        navController = navController,
        currentUser = null,
        onLogin = onLogin,
        onLogout = {}
    )
}

@Composable
fun MainApp(
    user: User,
    reminderRoute: String? = null,
    onReminderConsumed: () -> Unit = {},
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    com.intuthuko.smartreminder.ui.components.ReminderPermissionPrompt(user)
    LaunchedEffect(reminderRoute) {
        if (reminderRoute != null) {
            navController.navigate(reminderRoute) { launchSingleTop = true }
            onReminderConsumed()
        }
    }
    val navItems = navItemsForRole(user.role)

    val showBottomBar = currentRoute != null && currentRoute != Screen.Login.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp)) },
                            label = {
                                Text(
                                    item.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (currentRoute == item.screen.route) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = SecondaryText,
                                unselectedTextColor = SecondaryText,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            com.intuthuko.smartreminder.ui.components.CloudStatus(user)
            Box(Modifier.weight(1f)) {
            NavGraph(
                navController = navController,
                currentUser = user,
                onLogin = {},
                onLogout = onLogout
            )
            }
        }
    }
}

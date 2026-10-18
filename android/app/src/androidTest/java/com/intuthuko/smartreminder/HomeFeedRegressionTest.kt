package com.intuthuko.smartreminder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.intuthuko.smartreminder.data.LocalStore
import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.ui.screens.home.HomeScreen
import com.intuthuko.smartreminder.ui.theme.CampusSyncTheme
import org.junit.Rule
import org.junit.Test

class HomeFeedRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun overlappingTaskAndAnnouncementIdsCanScrollBothDirections() {
        LocalStore.initialize(ApplicationProvider.getApplicationContext())
        val originalTasks = MockData.tasks.toList()
        val originalAnnouncements = MockData.announcements.toList()
        val user = User(991, "Scroll Test", "scroll@example.invalid", "", UserRole.Student)
        try {
            // Independent tables can legitimately have the same primary keys.
            MockData.tasks.clear()
            MockData.tasks.addAll((1..3).map { Task(it, user.userId, user.userId, "Collision task $it", dueDate = "2026-10-25") })
            MockData.announcements.clear()
            MockData.announcements.addAll((1..2).map { Announcement(it, "Collision announcement $it", "Regression fixture", "2026-10-18") })
            compose.setContent { CampusSyncTheme { HomeScreen(user, rememberNavController()) } }
            repeat(3) {
                compose.onNode(hasScrollAction()).performScrollToNode(hasText("Collision announcement 2"))
                compose.onNodeWithText("Collision announcement 2").assertIsDisplayed()
                compose.onNode(hasScrollAction()).performScrollToNode(hasText("CampusSync"))
                compose.onNodeWithText("CampusSync").assertIsDisplayed()
            }
        } finally {
            compose.runOnIdle {
                MockData.tasks.clear(); MockData.tasks.addAll(originalTasks)
                MockData.announcements.clear(); MockData.announcements.addAll(originalAnnouncements)
            }
        }
    }
}

package com.intuthuko.smartreminder

import com.intuthuko.smartreminder.data.model.UserRole
import com.intuthuko.smartreminder.ui.navigation.Screen
import com.intuthuko.smartreminder.ui.navigation.navItemsForRole
import org.junit.Assert.*
import org.junit.Test

class RoleNavigationTest {
    @Test fun studentCannotOpenAdministrationFromNavigation() {
        val routes = navItemsForRole(UserRole.Student).map { it.screen.route }
        assertTrue(routes.containsAll(listOf(Screen.Assignments.route, Screen.Calendar.route, Screen.Notes.route)))
        assertFalse(routes.contains(Screen.Admin.route))
    }

    @Test fun tutorHasPlannerAndCommunicationWithoutAdministration() {
        val routes = navItemsForRole(UserRole.Tutor).map { it.screen.route }
        assertTrue(routes.containsAll(listOf(Screen.Calendar.route, Screen.Updates.route, Screen.Notes.route)))
        assertFalse(routes.contains(Screen.Admin.route))
    }

    @Test fun adminCanReachManagementAndEveryRoleHasAnEscapeRoute() {
        assertTrue(navItemsForRole(UserRole.Admin).any { it.screen == Screen.Admin })
        UserRole.entries.forEach { role ->
            val routes = navItemsForRole(role).map { it.screen.route }
            assertTrue(routes.contains(Screen.Home.route))
            assertTrue(routes.contains(Screen.More.route))
            assertEquals(routes.size, routes.distinct().size)
        }
        assertEquals(3, UserRole.entries.map { navItemsForRole(it).map { item -> item.screen.route } }.distinct().size)
    }
}

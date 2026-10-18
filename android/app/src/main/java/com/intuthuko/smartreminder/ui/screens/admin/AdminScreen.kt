package com.intuthuko.smartreminder.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(user: User, onBack: () -> Unit = {}) {
    if (com.intuthuko.smartreminder.data.CloudStore.active) { CloudAdminPanel(onBack); return }
    val repository = remember { AppRepository() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var users by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getAllUsers()) }
    var subjects by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getAllSubjects()) }
    var allTasks by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(MockData.tasks.toList()) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var showAddSubject by remember { mutableStateOf(false) }

    val completedTasks = allTasks.count { it.status == TaskStatus.Completed }
    val rewardPoints = completedTasks * 10

    Scaffold(
        topBar = { AppHeader("Admin panel", "A clear view of your community.", Icons.Default.AdminPanelSettings, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = Accent,
                edgePadding = 0.dp,
                divider = { Divider(color = Gray100, thickness = 1.dp) },
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            height = 3.dp,
                            color = Accent
                        )
                    }
                }
            ) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Overview", modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal)
                }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Subjects", modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal)
                }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Users", modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal)
                }
                Tab(selectedTab == 3, onClick = { selectedTab = 3 }) {
                    Text("Tasks", modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal)
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(users.size, allTasks.size, completedTasks, rewardPoints, subjects.size)
                1 -> SubjectsTab(subjects, { subjectToDelete = it }, { showAddSubject = true })
                2 -> UsersTab(users, repository, user.userId, { userToDelete = it }) { users = repository.getAllUsers() }
                3 -> AdminTasksTab(allTasks, repository) { allTasks = MockData.tasks.toList() }
            }
        }
    }

    userToDelete?.let { u ->
        ConfirmDialog(
            message = "Delete user '${u.username}'? This cannot be undone.",
            onConfirm = { repository.deleteUser(u.userId); users = repository.getAllUsers(); userToDelete = null },
            onDismiss = { userToDelete = null }
        )
    }

    subjectToDelete?.let { s ->
        ConfirmDialog(
            message = "Delete '${s.subjectName}'? All enrollments will be removed.",
            onConfirm = { repository.deleteSubject(s.subjectId); subjects = repository.getAllSubjects(); subjectToDelete = null },
            onDismiss = { subjectToDelete = null }
        )
    }

    if (showAddSubject) {
        AddSubjectDialog(
            onDismiss = { showAddSubject = false },
            onCreated = { subjects = repository.getAllSubjects(); showAddSubject = false }
        )
    }
}

@Composable
fun OverviewTab(userCount: Int, taskCount: Int, completedCount: Int, rewardPoints: Int, subjectCount: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(CobaltBlue, CobaltBlueDark)))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Platform Overview", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OverviewStat("Users", userCount.toString())
                        OverviewStat("Tasks", taskCount.toString())
                        OverviewStat("Done", completedCount.toString())
                        OverviewStat("Pts", rewardPoints.toString())
                    }
                }
            }
        } }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(18.dp)).background(WarningBackground), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MenuBook, null, tint = Warning, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Subjects", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Dark)
                        Text("$subjectCount available on platform", fontSize = 13.sp, color = Gray400)
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun SubjectsTab(subjects: List<Subject>, onDelete: (Subject) -> Unit, onAdd: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Subjects", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Dark)
                Button(onClick = onAdd, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(subjects) { subject ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subject.subjectName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dark)
                        Text("R${String.format("%.0f", subject.monthlyFee)}/month", fontSize = 13.sp, color = Gray400)
                    }
                    FilledTonalIconButton(
                        onClick = { onDelete(subject) },
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DangerLight, contentColor = Danger)
                    ) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
fun UsersTab(users: List<User>, repository: AppRepository, currentUserId: Int, onDelete: (User) -> Unit, onRefresh: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = users.filter {
        searchQuery.isEmpty() || it.username.contains(searchQuery, true) || it.email.contains(searchQuery, true) || it.role.displayName.contains(searchQuery, true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search users...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Gray400, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Gray200,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
        }
        items(filteredUsers) { u ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.username, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dark)
                            Text(u.email, fontSize = 13.sp, color = Gray400)
                        }
                        RoleBadge(u.role.displayName)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Gray100)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val enrolled = repository.getEnrolledSubjects(u.userId)
                        val taskCount = repository.getTasksForUser(u.userId).size
                        val pts = repository.getRewardPoints(u.userId)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStat("Subs", "${enrolled.size}")
                            MiniStat("Tasks", "$taskCount")
                            MiniStat("Pts", "$pts")
                        }
                        if (u.userId != currentUserId) {
                            FilledTonalIconButton(
                                onClick = { onDelete(u) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DangerLight, contentColor = Danger)
                            ) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", fontSize = 11.sp, color = Gray400)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Dark)
    }
}

@Composable
fun AdminTasksTab(tasks: List<Task>, repository: AppRepository, onRefresh: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTasks = tasks.filter { searchQuery.isEmpty() || it.title.contains(searchQuery, true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Gray400, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Gray200,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
        }
        items(filteredTasks) { task ->
            val taskUser = repository.getUser(task.userId)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dark)
                            Text("By: ${taskUser?.username ?: "Unknown"}", fontSize = 13.sp, color = Gray400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PriorityBadge(task.priority.displayName)
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(task.status.displayName)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = Gray400, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Due: ${task.dueDate}", fontSize = 13.sp, color = Gray400)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (task.status != TaskStatus.Completed) {
                                FilledTonalIconButton(
                                    onClick = { repository.updateTask(task.copy(status = TaskStatus.Completed)); onRefresh() },
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SuccessBackground, contentColor = Success)
                                ) { Icon(Icons.Default.Check, "Complete", modifier = Modifier.size(16.dp)) }
                            }
                            FilledTonalIconButton(
                                onClick = { repository.deleteTask(task.taskId); onRefresh() },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DangerLight, contentColor = Danger)
                            ) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val repository = remember { AppRepository() }
    var name by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }

    AppDialog(title = "Add Subject", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(160) }, label = { Text("Subject Name *") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Gray200, focusedContainerColor = Gray50, unfocusedContainerColor = Gray50)
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = fee, onValueChange = { fee = it }, label = { Text("Monthly Fee (ZAR) *") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Gray200, focusedContainerColor = Gray50, unfocusedContainerColor = Gray50)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val feeValue = fee.toDoubleOrNull()
                    if (name.isNotBlank() && feeValue != null) {
                        repository.addSubject(Subject(0, name, feeValue))
                        onCreated()
                    }
                },
                enabled = name.isNotBlank() && fee.toDoubleOrNull()?.let { it.isFinite() && it >= 0 && it <= 1000000 } == true && repository.getAllSubjects().none { it.subjectName.equals(name.trim(), true) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Add", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold) }
        }
    }
}


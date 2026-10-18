package com.intuthuko.smartreminder.ui.screens.assignments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*
import java.time.LocalDate

enum class AssignmentFilter(val displayName: String) { All("All"), Today("Today"), Upcoming("Upcoming"), Done("Done") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(user: User) {
    val repository = remember { AppRepository() }
    var tasks by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getTasksForUser(user.userId)) }
    var selectedFilter by remember { mutableStateOf(AssignmentFilter.All) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenuTaskId by remember { mutableIntStateOf(-1) }

    val todayStr = LocalDate.now().toString()
    val filteredTasks = remember(tasks, selectedFilter, searchQuery) {
        val filtered = when (selectedFilter) {
            AssignmentFilter.All -> tasks
            AssignmentFilter.Today -> tasks.filter { it.dueDate == todayStr }
            AssignmentFilter.Upcoming -> tasks.filter { it.status != TaskStatus.Completed && it.dueDate >= todayStr }
            AssignmentFilter.Done -> tasks.filter { it.status == TaskStatus.Completed || it.status == TaskStatus.Submitted }
        }
        if (searchQuery.isNotBlank()) filtered.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
        else filtered
    }

    Scaffold(
        topBar = { AppHeader("Assignments", "A little focus. A lot of progress.", Icons.Default.Assignment) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingTask = null; showCreateDialog = true },
                containerColor = CobaltBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(Icons.Default.Add, "Create Assignment")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search assignments...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite, cursorColor = CobaltBlue)
            )

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssignmentFilter.entries.forEach { filter ->
                    FilterChipItem(
                        label = "${filter.displayName} (${when (filter) {
                            AssignmentFilter.All -> tasks.size
                            AssignmentFilter.Today -> tasks.count { it.dueDate == todayStr }
                            AssignmentFilter.Upcoming -> tasks.count { it.status != TaskStatus.Completed && it.dueDate >= todayStr }
                            AssignmentFilter.Done -> tasks.count { it.status == TaskStatus.Completed || it.status == TaskStatus.Submitted }
                        }})",
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                EmptyState(
                    icon = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.Assignment,
                    title = if (searchQuery.isNotBlank()) "No results found" else "No assignments",
                    subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap + to create your first assignment"
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTasks, key = { it.taskId }) { task ->
                        val isCompleted = task.status == TaskStatus.Completed || task.status == TaskStatus.Submitted
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isCompleted,
                                            onCheckedChange = {
                                                tasks = tasks.map { if (it.taskId == task.taskId) it.copy(status = if (isCompleted) TaskStatus.Pending else TaskStatus.Completed) else it }
                                                repository.updateTask(task.copy(status = if (isCompleted) TaskStatus.Pending else TaskStatus.Completed))
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                                        )
                                        Column {
                                            Text(task.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (isCompleted) Gray400 else MainText)
                                            if (task.description.isNotEmpty()) {
                                                Text(task.description, fontSize = 13.sp, color = SecondaryText, maxLines = 2)
                                            }
                                        }
                                    }
                                    Box {
                                        IconButton(onClick = { showMenuTaskId = if (showMenuTaskId == task.taskId) -1 else task.taskId }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(18.dp))
                                        }
                                        DropdownMenu(expanded = showMenuTaskId == task.taskId, onDismissRequest = { showMenuTaskId = -1 }) {
                                            DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenuTaskId = -1; editingTask = task; showCreateDialog = true })
                                            DropdownMenuItem(text = { Text("Delete", color = Danger) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Danger) }, onClick = { showMenuTaskId = -1; taskToDelete = task })
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, null, tint = Gray400, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Due: ${task.dueDate}", fontSize = 13.sp, color = SecondaryText)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PriorityBadge(task.priority.displayName)
                                        StatusBadge(task.status.displayName)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAssignmentDialog(user = user, task = editingTask, onDismiss = { showCreateDialog = false }, onCreated = { tasks = repository.getTasksForUser(user.userId); showCreateDialog = false })
    }
    taskToDelete?.let { task ->
        ConfirmDialog(
            message = "Delete '${task.title}'? This action cannot be undone.",
            onConfirm = { repository.deleteTask(task.taskId); tasks = repository.getTasksForUser(user.userId); taskToDelete = null },
            onDismiss = { taskToDelete = null },
            confirmText = "Delete"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentDialog(user: User, task: Task? = null, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val repository = remember { AppRepository() }
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: LocalDate.now().plusDays(7).toString()) }
    var priority by remember { mutableStateOf(task?.priority ?: TaskPriority.Medium) }

    AppDialog(title = if (task == null) "New Assignment" else "Edit Assignment", onDismiss = onDismiss) {
        OutlinedTextField(value = title, onValueChange = { title = it.take(160) }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = description, onValueChange = { description = it.take(8000) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, isError = runCatching { LocalDate.parse(dueDate) }.isFailure, supportingText = { Text("Use a valid date, for example 2026-10-25") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(14.dp))
        Text("Priority", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MainText)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskPriority.entries.forEach { p ->
                FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.displayName, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = when (p) { TaskPriority.High -> HighPriorityBackground; TaskPriority.Medium -> WarningBackground; TaskPriority.Low -> SuccessBackground }, selectedLabelColor = when (p) { TaskPriority.High -> Danger; TaskPriority.Medium -> WarningAmber; TaskPriority.Low -> SuccessGreen }), shape = RoundedCornerShape(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(18.dp)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (title.isNotBlank()) {
                    if (task == null) repository.createTask(Task(0, user.userId, user.userId, title.trim(), description, dueDate, priority, TaskStatus.Pending, isSelfCreated = true)) else repository.updateTask(task.copy(title = title.trim(), description = description, dueDate = dueDate, priority = priority))
                    onCreated()
                }
            }, enabled = title.isNotBlank() && runCatching { LocalDate.parse(dueDate) }.isSuccess, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)) {
                Text(if (task == null) "Create" else "Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

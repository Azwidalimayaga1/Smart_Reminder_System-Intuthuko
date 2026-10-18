package com.intuthuko.smartreminder.ui.screens.gpa

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPACalculatorScreen(navController: NavController) {
    val repository = remember { AppRepository() }
    var modules by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getGPAModules()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var moduleToDelete by remember { mutableStateOf<GPAModule?>(null) }
    val gpa = remember(modules) { repository.calculateGPA() }

    Scaffold(
        topBar = { AppHeader("GPA calculator", "See the bigger picture.", Icons.Default.Calculate, onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = CobaltBlue, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(22.dp)) {
                Icon(Icons.Default.Add, "Add Module")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MidnightNavy)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Result", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                        Text("${String.format("%.2f", gpa)} GPA", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${modules.size} modules", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            if (modules.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(4.dp)); Text("Modules", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText) }
                items(modules, key = { it.moduleId }) { module ->
                    val gradePoint = when { module.mark >= 80 -> 4.0; module.mark >= 70 -> 3.0; module.mark >= 60 -> 2.0; module.mark >= 50 -> 1.0; else -> 0.0 }
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(module.moduleName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                                Text("${module.credits} credits • ${module.mark}%", fontSize = 13.sp, color = SecondaryText)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${String.format("%.1f", gradePoint)} pts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CobaltBlue)
                                IconButton(onClick = { moduleToDelete = module }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Remove", tint = Danger, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            } else {
                item { EmptyState(icon = Icons.Default.Calculate, title = "No modules added", subtitle = "Tap + to add your first module") }
            }
        }
    }

    if (showAddDialog) {
        AddModuleDialog(onDismiss = { showAddDialog = false }, onAdded = { modules = repository.getGPAModules(); showAddDialog = false })
    }
    moduleToDelete?.let { module ->
        ConfirmDialog(message = "Remove '${module.moduleName}'?", onConfirm = { repository.deleteGPAModule(module.moduleId); modules = repository.getGPAModules(); moduleToDelete = null }, onDismiss = { moduleToDelete = null }, confirmText = "Remove")
    }
}

@Composable
fun AddModuleDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    val repository = remember { AppRepository() }
    var name by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var mark by remember { mutableStateOf("") }

    AppDialog(title = "Add Module", onDismiss = onDismiss) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Module Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = credits, onValueChange = { credits = it.filter { c -> c.isDigit() } }, label = { Text("Credits *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = mark, onValueChange = { mark = it.filter { c -> c.isDigit() } }, label = { Text("Mark (%) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(18.dp)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val creditsInt = credits.toIntOrNull() ?: 0
                val markInt = mark.toIntOrNull() ?: 0
                if (name.isNotBlank() && creditsInt > 0 && markInt in 0..100) {
                    repository.addGPAModule(GPAModule(0, name, creditsInt, markInt))
                    onAdded()
                }
            }, enabled = name.isNotBlank() && (credits.toIntOrNull() ?: 0) in 1..1000 && mark.toIntOrNull() in 0..100, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy)) {
                Text("Add", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

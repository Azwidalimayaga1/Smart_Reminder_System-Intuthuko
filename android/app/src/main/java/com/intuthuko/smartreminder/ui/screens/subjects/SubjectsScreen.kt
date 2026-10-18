package com.intuthuko.smartreminder.ui.screens.subjects

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(user: User, onBack: () -> Unit = {}) {
    val repository = remember { AppRepository() }
    var enrolled by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getEnrolledSubjects(user.userId)) }
    var available by remember(com.intuthuko.smartreminder.data.LocalStore.revision) { mutableStateOf(repository.getAvailableSubjects(user.userId)) }
    var subjectToDrop by remember { mutableStateOf<Subject?>(null) }
    val monthlyTotal = enrolled.sumOf { it.monthlyFee }

    Scaffold(
        topBar = { AppHeader("My subjects", "${enrolled.size} enrolled • R${String.format("%.0f", monthlyTotal)}/month", Icons.Default.MenuBook, onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (enrolled.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Enrolled", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MainText)
                            Spacer(modifier = Modifier.height(12.dp))
                            enrolled.forEach { subject ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SubjectInitial(subject.subjectName, subjectColor(subject.subjectName), 32)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(subject.subjectName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MainText)
                                            Text("R${String.format("%.0f", subject.monthlyFee)}/month", fontSize = 13.sp, color = SecondaryText)
                                        }
                                    }
                                    OutlinedButton(onClick = { subjectToDrop = subject }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(32.dp), shape = RoundedCornerShape(8.dp)) {
                                        Text("Drop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item { EmptyState(icon = Icons.Default.MenuBook, title = "No subjects enrolled", subtitle = "Browse available subjects below") }
            }

            if (available.isNotEmpty()) {
                item { Text("Available Subjects", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MainText, modifier = Modifier.padding(top = 8.dp)) }
                items(available) { subject ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SubjectInitial(subject.subjectName, subjectColor(subject.subjectName), 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(subject.subjectName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                                Text("R${String.format("%.0f", subject.monthlyFee)}/month", fontSize = 13.sp, color = SecondaryText)
                            }
                            Button(onClick = {
                                repository.enrollSubject(user.userId, subject.subjectId)
                                enrolled = repository.getEnrolledSubjects(user.userId)
                                available = repository.getAvailableSubjects(user.userId)
                            }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), modifier = Modifier.height(36.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enroll", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    subjectToDrop?.let { subject ->
        ConfirmDialog(message = "Drop '${subject.subjectName}'? You will no longer be enrolled.", onConfirm = {
            repository.dropSubject(user.userId, subject.subjectId)
            enrolled = repository.getEnrolledSubjects(user.userId)
            available = repository.getAvailableSubjects(user.userId)
            subjectToDrop = null
        }, onDismiss = { subjectToDrop = null }, confirmText = "Drop")
    }
}

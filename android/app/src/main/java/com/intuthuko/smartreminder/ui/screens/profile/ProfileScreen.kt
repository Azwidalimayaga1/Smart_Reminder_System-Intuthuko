package com.intuthuko.smartreminder.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: User, navController: NavController) {
    val repository = remember { AppRepository() }
    var fullName by remember { mutableStateOf(user.username) }
    var studentNumber by remember { mutableStateOf(user.studentNumber) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var institution by remember { mutableStateOf(user.institution) }
    var course by remember { mutableStateOf(user.course) }
    var yearOfStudy by remember { mutableStateOf(user.yearOfStudy.toString()) }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppHeader("My profile", "Your details, your learning journey.", Icons.Default.Person, onBack = { navController.popBackStack() }) {                     TextButton(onClick = {
                        if (isEditing && (fullName.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || (yearOfStudy.toIntOrNull() ?: 0) !in 1..10)) { error = "Enter a name, valid email and year from 1 to 10." }
                        else if (isEditing && user.authUid.isNotEmpty() && email.trim() != user.email) { error = "This email is managed by your sign-in provider." }
                        else if (isEditing && repository.getAllUsers().any { it.userId != user.userId && it.email.equals(email.trim(), true) }) { error = "Email already registered." }
                        else if (isEditing) {
                            repository.updateUser((repository.getUser(user.userId) ?: user).copy(username = fullName.trim(), email = email.trim(), studentNumber = studentNumber, phone = phone, institution = institution, course = course, yearOfStudy = yearOfStudy.toIntOrNull() ?: 1))
                            error = null
                            isEditing = false
                        } else { isEditing = true }
                    }) {
                        Text(if (isEditing) "Save" else "Edit", color = CobaltBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    } } }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AvatarInitials(fullName, 72, CobaltBlue)
            Spacer(modifier = Modifier.height(12.dp))
            Text(fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MainText)
            Text(user.role.displayName, fontSize = 14.sp, color = SecondaryText)
            Spacer(modifier = Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ProfileField("Full Name", fullName, { fullName = it.take(160) }, isEditing, Icons.Default.Person)
                    ProfileField("Student Number", studentNumber, { studentNumber = it.take(80) }, isEditing, Icons.Default.Badge)
                    ProfileField("Email", email, { email = it }, isEditing, Icons.Default.Email)
                    ProfileField("Phone", phone, { phone = it.take(80) }, isEditing, Icons.Default.Phone)
                    ProfileField("Institution", institution, { institution = it.take(200) }, isEditing, Icons.Default.School)
                    ProfileField("Course", course, { course = it.take(200) }, isEditing, Icons.Default.MenuBook)
                    ProfileField("Year of Study", yearOfStudy, { yearOfStudy = it }, isEditing, Icons.Default.CalendarToday)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = { showPassword = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Password", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    if (showPassword) ChangePasswordDialog(user) { showPassword = false }
}

@Composable
fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit, isEditing: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    if (isEditing) {
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CobaltBlue, unfocusedBorderColor = BorderColor, focusedContainerColor = WarmBackground, unfocusedContainerColor = WarmBackground))
    } else {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 11.sp, color = SecondaryText, fontWeight = FontWeight.Medium)
                Text(value.ifEmpty { "Not set" }, fontSize = 14.sp, color = MainText, fontWeight = FontWeight.Medium)
            }
        }
    }
}

package com.intuthuko.smartreminder.ui.screens.login
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.intuthuko.smartreminder.data.AuthService
import com.intuthuko.smartreminder.data.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
private fun Context.activity(): Activity = when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> error("Open sign-in from the app.") }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: (User) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.Student) }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun run(action: suspend () -> Unit) { if (busy) return; busy = true; message = null; scope.launch { try { action() } catch (_: GetCredentialCancellationException) { message = "Google sign-in cancelled." } catch (e: CancellationException) { throw e } catch (e: Exception) { message = e.localizedMessage ?: "Please try again." } finally { busy = false } } }

    val configured = remember { AuthService.configured(context) }
    Column(Modifier.fillMaxSize().background(WarmBackground).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { CampusSyncLogo(40); Text("CampusSync", style = MaterialTheme.typography.titleLarge, color = MainText) }
        Surface(color = MidnightNavy, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.AutoAwesome, null, tint = HeroCoral, modifier = Modifier.size(18.dp)); Text("A LITTLE MORE IN SYNC", color = HeroMint, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold) }
                Text(if (register) "Your next chapter\nstarts here." else "Less to remember.\nMore room to grow.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Your assignments, notes and campus life.\nOne thoughtful space.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC6D8DC))
            }
        }
        Panel {
            Text(if (register) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineSmall, color = MainText)
            Text(if (register) "Start your student journey with CampusSync." else "Pick your role and sign in to your study space.", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
            if (!register) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { UserRole.entries.forEach { item ->
                FilterChip(selected = role == item, onClick = { role = item; message = null }, enabled = !busy, label = { Text(item.displayName) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp))
            } }
            if (register) OutlinedTextField(name, { name = it; message = null }, label = { Text("Full name") }, leadingIcon = { Icon(Icons.Default.PersonOutline, null) }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(email, { email = it; message = null }, label = { Text("Email address") }, leadingIcon = { Icon(Icons.Default.AlternateEmail, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(password, { password = it; message = null }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (show) "Hide password" else "Show password") } })
            if (register) OutlinedTextField(confirm, { confirm = it; message = null }, label = { Text("Confirm password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, shape = RoundedCornerShape(16.dp), visualTransformation = PasswordVisualTransformation(), supportingText = { Text("Use at least 8 characters.") })
            if (!register) TextButton(onClick = { run { AuthService.reset(context, email); message = "If this account supports password reset, a reset email will arrive shortly." } }, enabled = !busy, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?") }
            message?.let { Surface(color = HighPriorityBackground, shape = RoundedCornerShape(12.dp)) { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(12.dp)) } }
            Button(onClick = { run {
                val user = if (register) { require(password == confirm) { "Passwords do not match." }; AuthService.register(context, name, email, password) } else AuthService.login(context, email, password, role)
                onLogin(user)
            } }, enabled = !busy && email.isNotBlank() && password.isNotBlank(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(10.dp)) }
                Text(if (busy) "Please wait…" else if (register) "Create account" else "Sign in"); Spacer(Modifier.width(10.dp)); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Divider(Modifier.weight(1f), color = BorderColor); Text("or", color = SecondaryText, style = MaterialTheme.typography.bodySmall); Divider(Modifier.weight(1f), color = BorderColor) }
            OutlinedButton(onClick = { run { onLogin(AuthService.google(context.activity())) } }, enabled = !busy, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("G", fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.width(12.dp)); Text("Continue with Google", color = MainText) }
            TextButton(onClick = { register = !register; message = null; password = ""; confirm = "" }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Already have an account? Sign in" else "New here? Create an account") }
        }
        if (!configured) {
            Surface(color = LightBlue, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("You're exploring the demo", color = CobaltBlue, style = MaterialTheme.typography.titleSmall)
                    Text("Accounts and data stay on this device. Online sign-in isn't enabled yet.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    if (!register) Text("Student: student1@example.invalid\nTutor: tutor1@example.invalid • Admin: admin@example.invalid\nDemo password: pass123", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("A calmer way to stay on top of campus life.", style = MaterialTheme.typography.bodySmall, color = SecondaryText, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
    }
}

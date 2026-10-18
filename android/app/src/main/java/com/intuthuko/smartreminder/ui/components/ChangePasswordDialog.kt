package com.intuthuko.smartreminder.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.intuthuko.smartreminder.data.AuthService
import com.intuthuko.smartreminder.data.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
@Composable
fun ChangePasswordDialog(user: User, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Change password") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(current, { current = it }, label = { Text("Current password") }, visualTransformation = PasswordVisualTransformation(), enabled = !busy)
            OutlinedTextField(password, { password = it }, label = { Text("New password (8+ characters)") }, visualTransformation = PasswordVisualTransformation(), enabled = !busy)
            OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(), enabled = !busy)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = {
        if (password != confirm) { error = "Passwords do not match." } else { busy = true; scope.launch { try { AuthService.changePassword(context, user, current, password); onDismiss() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.localizedMessage } finally { busy = false } } }
    }) { Text(if (busy) "Saving…" else "Save") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } })
}

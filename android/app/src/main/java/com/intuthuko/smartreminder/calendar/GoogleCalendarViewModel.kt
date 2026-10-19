package com.intuthuko.smartreminder.calendar

import android.accounts.Account
import android.app.Activity
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.intuthuko.smartreminder.data.model.User
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleCalendarViewModel : ViewModel() {
    var events by mutableStateOf<List<GoogleCalendarEvent>>(emptyList())
        private set
    var connected by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var updatedAt by mutableStateOf<String?>(null)
        private set
    private var generation = 0
    private var pending: Int? = null
    private var work: Job? = null

    private fun isCurrent(user: User): Boolean = user.authUid.isNotEmpty() &&
        runCatching { FirebaseAuth.getInstance().currentUser?.uid == user.authUid }.getOrDefault(false)

    fun connect(activity: Activity, user: User, launch: (IntentSenderRequest) -> Unit) {
        if (busy) return
        if (!isCurrent(user)) { error = "Sign in with a real campus account to connect Google Calendar."; return }
        val requestGeneration = ++generation
        busy = true; error = null
        work = viewModelScope.launch {
            try {
                val request = AuthorizationRequest.builder()
                    .setAccount(Account(user.email, "com.google"))
                    .setRequestedScopes(listOf(Scope(GoogleCalendarApi.READ_SCOPE))).build()
                val result = Identity.getAuthorizationClient(activity).authorize(request).await()
                if (requestGeneration != generation || !isCurrent(user)) return@launch
                if (result.hasResolution()) {
                    val resolution = result.pendingIntent ?: error("Missing Google permission request")
                    pending = requestGeneration
                    launch(IntentSenderRequest.Builder(resolution.intentSender).build())
                } else load(result, user, requestGeneration)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { fail(e, requestGeneration) }
        }
    }

    fun onPermissionResult(activity: Activity, user: User, resultCode: Int, data: Intent?) {
        val requestGeneration = pending ?: return
        pending = null
        if (requestGeneration != generation) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            busy = false
            error = "Calendar permission was cancelled. Your campus account is still signed in."
            return
        }
        work = viewModelScope.launch {
            try {
                val result = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
                load(result, user, requestGeneration)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { fail(e, requestGeneration) }
        }
    }

    private suspend fun load(result: AuthorizationResult, user: User, requestGeneration: Int) {
        if (!isCurrent(user) || requestGeneration != generation) { disconnect(); return }
        require(result.grantedScopes.contains(GoogleCalendarApi.READ_SCOPE)) { "Calendar permission was not granted." }
        val token = result.accessToken ?: error("Google did not return calendar access.")
        val incoming = GoogleCalendarApi.upcoming(token)
        if (isCurrent(user) && requestGeneration == generation) {
            events = incoming; connected = true; busy = false
            updatedAt = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    private fun fail(e: Exception, requestGeneration: Int) {
        if (requestGeneration != generation) return
        busy = false
        error = when (e) {
            is CalendarApiException -> when {
                e.status == 401 -> "Google access has expired. Tap Refresh or reconnect to try again."
                e.status == 403 && e.reason in listOf("accessNotConfigured", "serviceDisabled") -> "Google Calendar API needs to be enabled for this app. Please contact your project administrator."
                e.status == 403 -> "Calendar access was denied. Reconnect and allow read-only calendar access. Your administrator may also need to add you as a Google OAuth test user."
                e.status == 429 -> "Google Calendar is receiving too many requests. Please wait a moment and refresh."
                e.status >= 500 -> "Google Calendar is temporarily unavailable. Please try again shortly."
                else -> "Could not load Google Calendar. Please reconnect and try again."
            }
            is ApiException -> when (e.statusCode) {
                12501, 16 -> "Calendar permission was cancelled. You can connect whenever you’re ready."
                10 -> "Google Calendar sign-in is not configured for this app’s signing certificate. Please contact your project administrator."
                7 -> "No internet connection. Reconnect and try again."
                else -> "Could not connect to Google Calendar. Check your internet connection and try again."
            }
            is IOException -> "Could not reach Google Calendar. Check your internet connection and refresh."
            is IllegalArgumentException -> "Read-only calendar permission is required to show your events."
            else -> "Could not connect to Google Calendar. Please try again."
        }
    }

    fun disconnect() {
        generation++; pending = null; work?.cancel(); work = null
        events = emptyList(); connected = false; busy = false; error = null; updatedAt = null
    }

    override fun onCleared() { disconnect(); super.onCleared() }
}

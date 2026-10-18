package com.intuthuko.smartreminder.data
import android.app.Activity
import android.content.Context
import com.intuthuko.smartreminder.BuildConfig
import kotlinx.coroutines.CancellationException
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.repository.AppRepository
import kotlinx.coroutines.tasks.await
object AuthService {
    fun configured(context: Context) = FirebaseApp.initializeApp(context) != null
    private fun local(firebase: com.google.firebase.auth.FirebaseUser): User {
        MockData.users.find { it.authUid == firebase.uid }?.let { return it }
        // Never grant a Google account a demo user's role just because emails match.
        require(MockData.users.none { it.email.equals(firebase.email, true) }) { "This email belongs to a local account. Use a different Google account or contact support to migrate it." }
        return MockData.addUser(User(0, firebase.displayName ?: firebase.email!!.substringBefore("@"), firebase.email!!, "", UserRole.Student, authUid = firebase.uid))
    }
    suspend fun login(context: Context, email: String, password: String, role: UserRole): User {
        val localAccount = MockData.users.find { it.authUid.isEmpty() && it.email.equals(email.trim(), true) }
        // Keep existing device-only demo accounts usable in debug builds after Firebase is configured.
        if (BuildConfig.DEBUG && localAccount != null) {
            val user = AppRepository().authenticate(email.trim(), password, role) ?: error("Invalid email, password or role.")
            signOut()
            return user
        }
        require(configured(context)) { "Online sign-in is not configured for this build." }
        val auth = FirebaseAuth.getInstance()
        val firebase = auth.signInWithEmailAndPassword(email.trim(), password).await().user ?: error("Sign-in failed.")
        val user = try { CloudStore.open(local(firebase)) } catch (error: Exception) { auth.signOut(); throw error }
        if (user.role != role) { auth.signOut(); error("Select the ${user.role.displayName} role for this account.") }
        return user
    }
    suspend fun register(context: Context, name: String, email: String, password: String): User {
        require(name.trim().length >= 3) { "Enter a name of at least 3 characters." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Enter a valid email." }
        require(password.length >= 8) { "Use at least 8 characters for your password." }
        require(MockData.users.none { it.email.equals(email.trim(), true) }) { "This email is already registered." }
        if (!configured(context)) {
            require(BuildConfig.DEBUG) { "Online registration is not configured for this build." }
            return AppRepository().register(name, email, password, UserRole.Student) ?: error("Name or email already registered.")
        }
        val firebase = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password).await().user ?: error("Registration failed.")
        firebase.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        return CloudStore.open(local(firebase))
    }
    suspend fun google(activity: Activity): User {
        require(configured(activity)) { "Google sign-in is not enabled yet. Please contact the app administrator." }
        val id = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        require(id != 0) { "Enable Google in Firebase Authentication and download the updated google-services.json." }
        val option = GetSignInWithGoogleOption.Builder(activity.getString(id)).build()
        val response = CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        require(response.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Unsupported sign-in credential." }
        val token = GoogleIdTokenCredential.createFrom(response.credential.data).idToken
        val firebase = FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user ?: error("Google sign-in failed.")
        return try { CloudStore.open(local(firebase)) } catch (error: Exception) { FirebaseAuth.getInstance().signOut(); throw error }
    }
    suspend fun clearGoogleSession(context: Context) {
        try { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Firebase is already signed out; provider cleanup is best effort. */ }
    }
    suspend fun reset(context: Context, email: String) {
        require(configured(context)) { "Password reset emails are not available in demo mode. Contact the app administrator for account help." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Enter your account email." }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim()).await()
    }
    suspend fun changePassword(context: Context, user: User, current: String, password: String) {
        require(password.length >= 8) { "Use at least 8 characters." }
        if (user.authUid.isNotEmpty()) {
            val firebase = FirebaseAuth.getInstance().currentUser ?: error("Please sign in again.")
            require(firebase.uid == user.authUid) { "Please sign in again." }
            require(firebase.providerData.any { it.providerId == "password" }) { "Your password is managed by Google. Change it in your Google account." }
            firebase.reauthenticate(EmailAuthProvider.getCredential(user.email, current)).await()
            firebase.updatePassword(password).await()
        } else {
            val latest = AppRepository().getUser(user.userId) ?: error("Account not found.")
            require(Passwords.verify(current, latest.password)) { "Current password is incorrect." }
            AppRepository().updateUser(latest.copy(password = Passwords.hash(password)))
        }
    }
    fun signOut() { com.intuthuko.smartreminder.reminders.ReminderScheduler.clear(); CloudStore.close(); runCatching { FirebaseAuth.getInstance().signOut() } }
}

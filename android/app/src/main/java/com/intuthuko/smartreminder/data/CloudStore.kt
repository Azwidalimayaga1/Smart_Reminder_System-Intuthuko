package com.intuthuko.smartreminder.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.model.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Personal records live under the authenticated UID. Passwords and roles never enter this payload. */
object CloudStore {
    private val gson = Gson()
    private val db get() = FirebaseFirestore.getInstance()
    private var owner: User? = null
    private var deviceSnapshot: JsonObject? = null
    private var generation = ""
    private val listeners = mutableListOf<ListenerRegistration>()
    private val bindings = mutableMapOf<String, String>()
    private var baseline = mapOf<String, Map<String, String>>()
    private var profileBaseline = emptyMap<String, Any>()
    private var applying = false
    private var nextId = 1_000_000
    private var pending = 0
    private var cached = false
    private val enrolledFallback = mutableMapOf<Int, Subject>()
    var error by mutableStateOf<String?>(null)
        private set
    var status by mutableStateOf("Saved on this device")
        private set
    val active get() = owner != null
    val userId get() = owner?.userId
    private val kinds = mapOf(
        "tasks" to ("taskId" to Task::class.java),
        "notes" to ("noteId" to Note::class.java),
        "calendarEvents" to ("eventId" to CalendarEvent::class.java),
        "notifications" to ("notificationId" to Notification::class.java),
        "userSubjects" to ("subjectId" to UserSubject::class.java),
        "gpaModules" to ("moduleId" to GPAModule::class.java)
    )

    private fun profile(user: User): Map<String, Any> = mapOf(
        "username" to user.username, "email" to user.email, "studentNumber" to user.studentNumber,
        "phone" to user.phone, "institution" to user.institution, "course" to user.course,
        "yearOfStudy" to user.yearOfStudy
    )
    private fun withProfile(user: User, data: Map<String, Any>): User = user.copy(
        username = data["username"] as? String ?: user.username,
        email = data["email"] as? String ?: user.email,
        studentNumber = data["studentNumber"] as? String ?: "",
        phone = data["phone"] as? String ?: "",
        institution = data["institution"] as? String ?: "",
        course = data["course"] as? String ?: "",
        yearOfStudy = (data["yearOfStudy"] as? Number)?.toInt() ?: 1
    )
    private fun updateProfile(user: User) {
        owner = user
        val index = MockData.users.indexOfFirst { it.authUid == user.authUid }
        if (index >= 0) MockData.users[index] = user
        LocalStore.changed()
    }

    suspend fun open(user: User): User {
        require(FirebaseAuth.getInstance().currentUser?.uid == user.authUid) { "Please sign in again." }
        if (owner?.authUid == user.authUid) return owner!!
        close()
        // Resolve authority from server before displaying privileged screens.
        val roleDoc = db.collection("roles").document(user.authUid).get(Source.SERVER).await()
        require(roleDoc.getBoolean("disabled") != true) { "This account is disabled. Contact your campus administrator." }
        val role = roleDoc.getString("role")?.let { name -> UserRole.entries.find { it.name == name } } ?: UserRole.Student
        val profileRef = db.collection("profiles").document(user.authUid)
        var profileDoc = profileRef.get(Source.SERVER).await()
        if (!profileDoc.exists()) {
            // A transaction prevents a second device from overwriting an existing profile.
            db.runTransaction { transaction ->
                if (!transaction.get(profileRef).exists()) transaction.set(profileRef, profile(user))
            }.await()
            profileDoc = profileRef.get(Source.SERVER).await()
        }
        val account = db.collection("accounts").document(user.authUid)
        val previous = LocalStore.snapshot()
        val signedIn = withProfile(user.copy(role = role), profileDoc.data ?: emptyMap())
        // Only this person's explicitly owned records migrate. Unowned demo GPA data stays local.
        val migration = previous.deepCopy()
        kinds.keys.forEach { kind ->
            val array = com.google.gson.JsonArray()
            if (kind != "gpaModules") previous.getAsJsonArray(kind)?.forEach { item ->
                if (item.asJsonObject["userId"]?.asInt == user.userId) array.add(item.deepCopy())
            }
            migration.add(kind, array)
        }
        val initial = mutableMapOf<String, Map<String, String>>()
        kinds.forEach { (kind, spec) -> migration.getAsJsonArray(kind).forEach { element ->
            val payload = element.asJsonObject.deepCopy()
            payload.remove(spec.first)
            payload.remove("userId")
            payload.remove("assignedBy")
            // Enrolment references retain the shared subject identifier.
            if (kind == "userSubjects") {
                val subjectId = element.asJsonObject["subjectId"].asInt
                payload.addProperty("subjectId", subjectId)
                MockData.subjects.find { it.subjectId == subjectId }?.let {
                    payload.addProperty("subjectName", it.subjectName); payload.addProperty("monthlyFee", it.monthlyFee)
                }
            }
            val documentId = if (kind == "userSubjects") "enrolment_${payload["subjectId"].asInt}" else UUID.randomUUID().toString()
            initial[documentId] = mapOf("kind" to kind, "payload" to payload.toString())
        } }
        require(initial.size < 400) { "Your existing data needs a larger migration. Please contact support; nothing has been changed." }
        // A permanent marker distinguishes a genuinely new account from one whose records were deleted.
        db.runTransaction { transaction ->
            if (!transaction.get(account).exists()) {
                transaction.set(account, mapOf("schemaVersion" to 1))
                initial.forEach { (id, data) -> transaction.set(account.collection("items").document(id), data) }
            }
        }.await()
        val records = account.collection("items").get(Source.SERVER).await()
        deviceSnapshot = previous
        owner = signedIn
        generation = UUID.randomUUID().toString()
        val session = generation
        profileBaseline = profile(signedIn)
        error = null
        applyRecords(records)
        MockData.subjects.clear(); MockData.timetableEntries.clear(); MockData.announcements.clear()
        updateProfile(signedIn)
        listeners += account.collection("items").addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, failure ->
            if (session == generation) {
                if (failure != null) failed() else if (snapshot != null) {
                    cached = snapshot.metadata.isFromCache
                    applyRecords(snapshot); updateStatus()
                }
            }
        }
        listeners += profileRef.addSnapshotListener { snapshot, failure ->
            if (session == generation) {
                if (failure != null) failed() else if (snapshot?.exists() == true) {
                    val current = owner ?: return@addSnapshotListener
                    val updated = withProfile(current, snapshot.data ?: emptyMap())
                    profileBaseline = profile(updated)
                    updateProfile(updated)
                }
            }
        }
        listeners += db.collection("roles").document(user.authUid).addSnapshotListener { snapshot, failure ->
            if (session == generation) {
                if (failure != null) failed() else {
                    val current = owner ?: return@addSnapshotListener
                    val resolved = snapshot?.getString("role")?.let { value -> UserRole.entries.find { it.name == value } } ?: UserRole.Student
                    if (snapshot?.getBoolean("disabled") == true) {
                        AuthService.signOut()
                    } else updateProfile(current.copy(role = resolved))
                }
            }
        }
        listenCatalog("subjects", Subject::class.java, MockData.subjects, session)
        listenCatalog("timetable", TimetableEntry::class.java, MockData.timetableEntries, session)
        listenCatalog("announcements", Announcement::class.java, MockData.announcements, session)
        updateStatus()
        return owner ?: error("Please sign in again.")
    }

    private fun <T> listenCatalog(kind: String, type: Class<T>, target: MutableList<T>, session: String) {
        listeners += db.collection(kind).addSnapshotListener { snapshot, failure ->
            if (session == generation) {
                if (failure != null) failed() else if (snapshot != null) {
                    val values = runCatching { snapshot.documents.map { gson.fromJson(gson.toJson(it.data), type) } }
                    values.onSuccess { target.clear(); target.addAll(it); LocalStore.changed() }.onFailure { failed() }
                }
            }
        }
    }

    private fun applyRecords(snapshot: QuerySnapshot) {
        val current = owner ?: return
        try {
            applying = true
            val data = LocalStore.snapshot()
            kinds.keys.forEach { data.add(it, com.google.gson.JsonArray()) }
            val nextBaseline = mutableMapOf<String, Map<String, String>>()
            enrolledFallback.clear()
            snapshot.documents.forEach { doc ->
                val kind = doc.getString("kind") ?: return@forEach
                val spec = kinds[kind] ?: return@forEach
                val payload = doc.getString("payload") ?: return@forEach
                val json = gson.fromJson(payload, JsonObject::class.java)
                val id = if (kind == "userSubjects") json["subjectId"].asInt else {
                    val binding = bindings.entries.find { it.key.startsWith("$kind:") && it.value == doc.id }
                    binding?.key?.substringAfter(":")?.toInt() ?: allocateId()
                }
                bindings["$kind:$id"] = doc.id
                if (kind == "userSubjects" && json.has("subjectName")) {
                    enrolledFallback[id] = Subject(id, json["subjectName"].asString, json["monthlyFee"]?.asDouble ?: 0.0)
                }
                if (kind != "userSubjects") json.addProperty(spec.first, id)
                if (kind != "gpaModules") json.addProperty("userId", current.userId)
                if (kind == "tasks") json.addProperty("assignedBy", current.userId)
                data.getAsJsonArray(kind).add(json)
                nextBaseline[doc.id] = mapOf("kind" to kind, "payload" to payload)
            }
            baseline = nextBaseline
            LocalStore.restore(data)
        } catch (_: Exception) { failed() } finally { applying = false }
    }

    fun save() {
        val current = owner ?: return
        if (applying) return
        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        if (authUid != current.authUid) { failed(); return }
        val session = generation
        val snapshot = LocalStore.snapshot()
        val next = mutableMapOf<String, Map<String, String>>()
        kinds.forEach { (kind, spec) -> snapshot.getAsJsonArray(kind).forEach record@ { element ->
            val payload = element.asJsonObject.deepCopy()
            if (kind != "gpaModules" && payload["userId"]?.asInt != current.userId) return@record
            val id = payload[spec.first].asInt
            val documentId = bindings.getOrPut("$kind:$id") { if (kind == "userSubjects") "enrolment_$id" else UUID.randomUUID().toString() }
            if (kind != "userSubjects") payload.remove(spec.first)
            payload.remove("userId"); payload.remove("assignedBy")
            if (kind == "userSubjects") {
                (MockData.subjects.find { it.subjectId == id } ?: enrolledFallback[id])?.let {
                    payload.addProperty("subjectName", it.subjectName); payload.addProperty("monthlyFee", it.monthlyFee)
                }
            }
            next[documentId] = mapOf("kind" to kind, "payload" to payload.toString())
        } }
        val changed = next.filter { (id, data) -> baseline[id] != data }
        val removed = baseline.keys - next.keys
        val updatedProfile = MockData.users.find { it.authUid == current.authUid } ?: current
        val profileData = profile(updatedProfile)
        val profileChanged = profileData != profileBaseline
        if (changed.isEmpty() && removed.isEmpty() && !profileChanged) return
        if (changed.values.any { it.getValue("payload").length > 60000 }) {
            error = "This record is too large to sync. Shorten it to fewer than 60,000 characters."
            return
        }
        val items = db.collection("accounts").document(current.authUid).collection("items")
        val operations = changed.map { (id, data) -> id to data } + removed.map { it to null }
        baseline = next; profileBaseline = profileData
        operations.chunked(350).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (id, data) -> if (data == null) batch.delete(items.document(id)) else batch.set(items.document(id), data) }
            pending++; updateStatus()
            batch.commit().addOnCompleteListener { task ->
                if (session == generation) { pending--; if (!task.isSuccessful) failed() else updateStatus() }
            }
        }
        if (profileChanged) {
            pending++; updateStatus()
            db.collection("profiles").document(current.authUid).set(profileData).addOnCompleteListener { task ->
                if (session == generation) { pending--; if (!task.isSuccessful) failed() else updateStatus() }
            }
        }
    }

    fun addSubject(subject: Subject) {
        val current = owner ?: return
        if (current.role != UserRole.Admin) { error = "Only an Admin can manage subjects."; return }
        val id = java.security.SecureRandom().nextInt(Int.MAX_VALUE - 1) + 1
        val ref = db.collection("subjects").document(id.toString())
        val data = subject.copy(subjectId = id)
        val session = generation
        db.runTransaction { tx ->
            require(!tx.get(ref).exists()) { "Please try adding this subject again." }
            tx.set(ref, mapOf("subjectId" to id, "subjectName" to data.subjectName.trim(), "monthlyFee" to data.monthlyFee))
        }.addOnFailureListener { if (session == generation) failed() }
    }
    fun deleteSubject(id: Int) {
        val session = generation
        db.collection("subjects").document(id.toString()).delete().addOnFailureListener { if (session == generation) failed() }
    }
    fun publishAnnouncement(title: String, message: String) {
        val current = owner ?: return
        if (current.role !in listOf(UserRole.Tutor, UserRole.Admin)) { error = "Only campus staff can publish updates."; return }
        val id = java.security.SecureRandom().nextInt(Int.MAX_VALUE - 1) + 1
        val session = generation
        db.collection("announcements").document(id.toString()).set(mapOf(
            "announcementId" to id, "title" to title.trim(), "message" to message.trim(),
            "timestamp" to java.time.LocalDate.now().toString(), "isDismissed" to false,
            "authorUid" to current.authUid
        )).addOnFailureListener { if (session == generation) failed() }
    }
    fun dismissError() { error = null }
    private fun allocateId(): Int {
        while (bindings.keys.any { it.substringAfter(":").toInt() == nextId }) nextId++
        return nextId++
    }
    fun enrolledSubject(id: Int): Subject? = enrolledFallback[id]
    fun reminderKey(kind: String, id: Int): String = bindings["$kind:$id"] ?: id.toString()
    private fun failed() { error = "Cloud sync could not complete. Check your connection and try again. Rejected changes will return to the last saved version."; status = "Cloud sync needs attention" }
    private fun updateStatus() {
        status = when { error != null -> "Cloud sync needs attention"; pending > 0 -> "Saving changes…"; cached -> "Offline · changes sync when connected"; else -> "Synced with your account" }
    }
    fun close() {
        generation = ""
        listeners.forEach { it.remove() }; listeners.clear()
        owner = null; bindings.clear(); enrolledFallback.clear(); baseline = emptyMap(); profileBaseline = emptyMap()
        pending = 0; cached = false; error = null; status = "Saved on this device"
        deviceSnapshot?.let { LocalStore.restore(it) }
        deviceSnapshot = null
    }
}

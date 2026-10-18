package com.intuthuko.smartreminder.data
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intuthuko.smartreminder.data.mock.MockData
object LocalStore {
    private lateinit var context: Context
    var revision by mutableIntStateOf(0)
        private set
    private val gson = Gson()
    private val listNames = setOf("users", "tasks", "subjects", "userSubjects", "calendarEvents", "notes", "notifications", "announcements", "gpaModules", "timetableEntries")
    val prefs get() = context.getSharedPreferences("campus_settings", Context.MODE_PRIVATE)
    fun initialize(ctx: Context) {
        if (::context.isInitialized) return
        context = ctx.applicationContext
        val saved = prefs.getString("data", null)
        if (saved != null) {
            val json = gson.fromJson(saved, JsonObject::class.java)
            MockData::class.java.declaredFields.filter { it.name in listNames }.forEach { field ->
                if (json.has(field.name)) {
                    field.isAccessible = true
                    @Suppress("UNCHECKED_CAST") val list = field.get(MockData) as MutableList<Any>
                    val restored: List<Any> = gson.fromJson(json[field.name], field.genericType)
                    list.clear(); list.addAll(restored)
                }
            }
        }
        MockData.users.replaceAll { if (it.password.isNotEmpty() && !it.password.startsWith("pbkdf2:")) it.copy(password = Passwords.hash(it.password)) else it }
        // Restore counters from records to prevent duplicate identifiers after restart.
        val counters = mapOf("nextUserId" to (MockData.users.maxOfOrNull { it.userId } ?: 0), "nextTaskId" to (MockData.tasks.maxOfOrNull { it.taskId } ?: 0), "nextEventId" to (MockData.calendarEvents.maxOfOrNull { it.eventId } ?: 0), "nextSubjectId" to (MockData.subjects.maxOfOrNull { it.subjectId } ?: 0), "nextNoteId" to (MockData.notes.maxOfOrNull { it.noteId } ?: 0), "nextNotificationId" to (MockData.notifications.maxOfOrNull { it.notificationId } ?: 0), "nextGpaModuleId" to (MockData.gpaModules.maxOfOrNull { it.moduleId } ?: 0))
        counters.forEach { (name, max) -> MockData::class.java.getDeclaredField(name).apply { isAccessible = true }.setInt(MockData, max + 1) }
        save()
    }
    fun snapshot(): JsonObject {
        val json = JsonObject()
        MockData::class.java.declaredFields.filter { it.name in listNames }.forEach { field -> field.isAccessible = true; json.add(field.name, gson.toJsonTree(field.get(MockData))) }
        return json
    }
    fun restore(json: JsonObject) {
        MockData::class.java.declaredFields.filter { it.name in listNames }.forEach { field ->
            if (json.has(field.name)) {
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST") val list = field.get(MockData) as MutableList<Any>
                val restored: List<Any> = gson.fromJson(json[field.name], field.genericType)
                list.clear(); list.addAll(restored)
            }
        }
        val counters = mapOf("nextUserId" to (MockData.users.maxOfOrNull { it.userId } ?: 0), "nextTaskId" to (MockData.tasks.maxOfOrNull { it.taskId } ?: 0), "nextEventId" to (MockData.calendarEvents.maxOfOrNull { it.eventId } ?: 0), "nextSubjectId" to (MockData.subjects.maxOfOrNull { it.subjectId } ?: 0), "nextNoteId" to (MockData.notes.maxOfOrNull { it.noteId } ?: 0), "nextNotificationId" to (MockData.notifications.maxOfOrNull { it.notificationId } ?: 0), "nextGpaModuleId" to (MockData.gpaModules.maxOfOrNull { it.moduleId } ?: 0))
        counters.forEach { (name, max) -> MockData::class.java.getDeclaredField(name).apply { isAccessible = true }.setInt(MockData, max + 1) }
        changed()
    }
    fun changed() { revision++; com.intuthuko.smartreminder.reminders.ReminderScheduler.dataChanged() }
    fun save() {
        if (!::context.isInitialized) return
        if (CloudStore.active) { CloudStore.save(); changed(); return }
        prefs.edit().putString("data", gson.toJson(snapshot())).putString("last_saved", java.time.LocalDateTime.now().toString()).apply()
        changed()
    }
}

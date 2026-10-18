package com.intuthuko.smartreminder.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intuthuko.smartreminder.MainActivity
import com.intuthuko.smartreminder.R
import com.intuthuko.smartreminder.data.CloudStore
import com.intuthuko.smartreminder.data.mock.MockData
import com.intuthuko.smartreminder.data.model.*
import java.time.*
import java.util.UUID

object ReminderScheduler {
    private const val STORE = "campus_reminders"
    private const val CHANNEL = "study_reminders_v1"
    const val OPEN = "com.intuthuko.smartreminder.OPEN_REMINDER"
    private var appContext: Context? = null
    private var activeUser: User? = null
    private val gson = Gson()
    private fun prefs(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
    private fun alarms(context: Context) = context.getSystemService(AlarmManager::class.java)
    fun identity(user: User) = if (user.authUid.isNotEmpty()) "firebase:${user.authUid}" else "local:${user.userId}"

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val channel = NotificationChannel(CHANNEL, "Study reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Upcoming assignments and calendar events"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    fun activate(user: User) {
        val context = appContext ?: return
        if (prefs(context).getString("owner", null) != identity(user)) clear(context)
        activeUser = user
        prefs(context).edit().putString("owner", identity(user)).commit()
        dataChanged()
    }
    fun enabled(context: Context, user: User) = prefs(context).getBoolean("enabled_${identity(user)}", true)
    fun setEnabled(context: Context, user: User, value: Boolean) {
        prefs(context).edit().putBoolean("enabled_${identity(user)}", value).commit()
        if (!value) {
            readPlans(context).forEach { cancelAlarm(context, it.key) }
            context.getSystemService(NotificationManager::class.java).cancelAll()
        }
        dataChanged()
    }
    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        && NotificationManagerCompat.from(context).areNotificationsEnabled()
        && context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun exact(context: Context) = Build.VERSION.SDK_INT < 31 || alarms(context).canScheduleExactAlarms()
    fun permissionAsked(context: Context) = prefs(context).getBoolean("permission_asked", false)
    fun markPermissionAsked(context: Context) { prefs(context).edit().putBoolean("permission_asked", true).apply() }

    fun dataChanged() {
        val context = appContext ?: return
        val user = activeUser ?: return
        if (prefs(context).getString("owner", null) != identity(user)) return
        if (user.authUid.isNotEmpty() && (!CloudStore.active || CloudStore.userId != user.userId)) return
        val settings = context.getSharedPreferences("campus_settings", Context.MODE_PRIVATE)
        val categories = NotificationCategory.entries.filter { settings.getBoolean("notification_${user.userId}_${it.name}", true) }.toSet()
        val generated = ReminderPlanner.plan(user.userId, identity(user), MockData.tasks.toList(), MockData.calendarEvents.toList(), Instant.now(), ZoneId.systemDefault(), categories,
            taskKey = { if (user.authUid.isNotEmpty()) CloudStore.reminderKey("tasks", it) else it.toString() },
            eventKey = { if (user.authUid.isNotEmpty()) CloudStore.reminderKey("calendarEvents", it) else it.toString() })
        val tests = readPlans(context).filter { it.key.startsWith("test:") && it.owner == identity(user) && it.expiresMillis > System.currentTimeMillis() }
        replacePlans(context, generated + tests)
    }
    private fun readPlans(context: Context): List<PlannedReminder> = runCatching {
        gson.fromJson<List<PlannedReminder>>(prefs(context).getString("plans", "[]"), object : TypeToken<List<PlannedReminder>>() {}.type)
    }.getOrDefault(emptyList())
    private fun delivered(context: Context, owner: String) = prefs(context).getStringSet("delivered_$owner", emptySet())!!.toSet()
    private fun replacePlans(context: Context, plans: List<PlannedReminder>) {
        val old = readPlans(context)
        val keys = plans.map { it.key }.toSet()
        old.filter { it.key !in keys }.forEach { cancelAlarm(context, it.key) }
        context.getSystemService(NotificationManager::class.java).activeNotifications.filter { it.tag?.startsWith("reminder:") == true && it.tag.removePrefix("reminder:") !in keys }
            .forEach { context.getSystemService(NotificationManager::class.java).cancel(it.tag, it.id) }
        prefs(context).edit().putString("plans", gson.toJson(plans)).commit()
        restore(context)
    }

    /** This path also runs after reboot, without loading any other person's workspace. */
    fun restore(context: Context) {
        initialize(context)
        val owner = prefs(context).getString("owner", null) ?: return
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val done = delivered(context, owner)
        val plans = readPlans(context).mapNotNull { plan ->
            runCatching {
                val expiry = plan.localExpiry?.let { LocalDateTime.parse(it).atZone(zone).toInstant().toEpochMilli() } ?: plan.expiresMillis
                val trigger = plan.localTime?.let { LocalDateTime.parse(it).atZone(zone).toInstant().toEpochMilli() } ?: plan.atMillis
                if (plan.owner != owner || expiry <= now) { cancelAlarm(context, plan.key); null }
                else plan.copy(atMillis = trigger.coerceAtLeast(now + 2000), expiresMillis = expiry)
            }.getOrNull()
        }
        prefs(context).edit().putString("plans", gson.toJson(plans)).commit()
        if (!prefs(context).getBoolean("enabled_$owner", true) || !canNotify(context) || !ownerMatches(context, owner)) {
            plans.forEach { cancelAlarm(context, it.key) }; return
        }
        val pending = plans.filter { it.key !in done }.sortedBy { it.atMillis }
        // Keep below device alarm limits; each delivery fills the window again.
        pending.drop(32).forEach { cancelAlarm(context, it.key) }
        pending.take(32).forEach { schedule(context, it) }
    }
    private fun ownerMatches(context: Context, owner: String): Boolean {
        if (owner.startsWith("local:")) return true
        return runCatching {
            FirebaseApp.initializeApp(context)
            FirebaseAuth.getInstance().currentUser?.uid == owner.removePrefix("firebase:")
        }.getOrDefault(false)
    }
    private fun alarmIntent(context: Context, key: String, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.intuthuko.smartreminder.REMIND"
            data = Uri.Builder().scheme("campussync").authority("reminder").appendPath(key).build()
            putExtra("key", key)
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun cancelAlarm(context: Context, key: String) {
        alarmIntent(context, key, PendingIntent.FLAG_NO_CREATE)?.let { alarms(context).cancel(it); it.cancel() }
    }
    private fun schedule(context: Context, plan: PlannedReminder) {
        val operation = alarmIntent(context, plan.key, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (exact(context)) alarms(context).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.atMillis, operation)
            else alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.atMillis, operation)
        } catch (_: SecurityException) {
            // Exact-alarm access can be revoked between checking and scheduling.
            alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.atMillis, operation)
        }
    }
    fun scheduleTest(context: Context, user: User): Boolean {
        if (!enabled(context, user) || !canNotify(context)) return false
        activate(user)
        val now = System.currentTimeMillis()
        val test = PlannedReminder("test:${UUID.randomUUID()}", identity(user), user.userId, "CampusSync reminders are working",
            "You're ready for assignment and calendar alerts.", now + 20000, now + 10 * 60000, null, null, NotificationCategory.General.name, "home")
        replacePlans(context, readPlans(context).filterNot { it.key.startsWith("test:") } + test)
        return true
    }
    fun deliver(context: Context, key: String) {
        initialize(context)
        val plan = readPlans(context).find { it.key == key } ?: return
        val owner = prefs(context).getString("owner", null) ?: return
        if (owner != plan.owner || !ownerMatches(context, owner)) { clear(context); return }
        if (key in delivered(context, owner)) return
        val now = System.currentTimeMillis()
        if (now + 1000 < plan.atMillis) { schedule(context, plan); return }
        if (now >= plan.expiresMillis) { restore(context); return }
        if (!prefs(context).getBoolean("enabled_$owner", true) || !canNotify(context)) return
        val settings = context.getSharedPreferences("campus_settings", Context.MODE_PRIVATE)
        if (!key.startsWith("test:") && !settings.getBoolean("notification_${plan.userId}_${plan.category}", true)) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = OPEN
            data = Uri.Builder().scheme("campussync").authority("open-reminder").appendPath(key).build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_owner", owner); putExtra("reminder_route", plan.route)
        }
        val content = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val generic = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("CampusSync").setContentText("You have a study reminder.").build()
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(plan.title).setContentText(plan.message).setStyle(NotificationCompat.BigTextStyle().bigText(plan.message))
            .setContentIntent(content).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(generic).build()
        try {
            context.getSystemService(NotificationManager::class.java).notify("reminder:$key", 0, notification)
            val completed = (delivered(context, owner).toList() + key).takeLast(2000).toSet()
            prefs(context).edit().putStringSet("delivered_$owner", completed).apply()
            restore(context)
        } catch (_: SecurityException) { /* User disabled notification access. */ }
    }
    fun clear(context: Context? = appContext) {
        context ?: return
        readPlans(context).forEach { cancelAlarm(context, it.key) }
        context.getSystemService(NotificationManager::class.java).cancelAll()
        prefs(context).edit().remove("owner").remove("plans").commit()
        activeUser = null
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { intent.getStringExtra("key")?.let { ReminderScheduler.deliver(context, it) } }
}
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) ReminderScheduler.restore(context)
    }
}

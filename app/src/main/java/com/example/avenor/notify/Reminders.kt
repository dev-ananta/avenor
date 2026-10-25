package com.example.avenor.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.avenor.MainActivity
import com.example.avenor.R
import com.example.avenor.data.AvenorTask
import com.example.avenor.data.StudentProfile
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Local notifications only, scheduled on the phone with AlarmManager — no push server to pay for.
 * Same defaults as iOS: 7 PM the night before a due date, 7 AM the day of, one daily planning nudge,
 * and focus-session alerts.
 */
object Reminders {
    private const val CHANNEL_ID = "avenor_reminders"
    private const val FOCUS_END = "focus-end"
    private const val BREAK_END = "break-end"
    private const val COME_BACK = "focus-come-back"
    private const val DAILY_PLAN = "daily-plan"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Due dates, focus sessions and a daily planning nudge"
                }
            )
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // ---------- Tasks ----------

    private fun taskKeys(id: String) = listOf("task-$id-eve", "task-$id-day")

    fun cancelTask(context: Context, id: String) = taskKeys(id).forEach { cancel(context, it) }

    fun sync(context: Context, task: AvenorTask, enabled: Boolean) {
        cancelTask(context, task.id)
        val due = task.dueDate ?: return
        if (!enabled || task.completed) return
        val now = System.currentTimeMillis()
        val keys = taskKeys(task.id)
        val evening = at(due.minusDays(1), 19)
        if (evening > now) schedule(context, keys[0], evening, "Due tomorrow: ${task.title}",
            "One focus session tonight makes tomorrow easy. It's already queued in Avenor.")
        val morning = at(due, 7)
        if (morning > now) schedule(context, keys[1], morning, "Due today: ${task.title}",
            "Open Avenor and tap Start — it's at the top of your list.")
    }

    fun resyncAll(context: Context, tasks: List<AvenorTask>, profile: StudentProfile) {
        tasks.forEach { sync(context, it, profile.remindersEnabled) }
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, DAILY_PLAN, "Two-minute plan",
            "Check what's next and start one session. Small starts beat big plans.")
        alarm.cancel(pi)
        if (!profile.remindersEnabled) return
        var first = at(LocalDate.now(), profile.planReminderHour)
        if (first <= System.currentTimeMillis()) first = at(LocalDate.now().plusDays(1), profile.planReminderHour)
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, pi)
    }

    fun cancelAll(context: Context, tasks: List<AvenorTask>) {
        tasks.forEach { cancelTask(context, it.id) }
        listOf(FOCUS_END, BREAK_END, COME_BACK, DAILY_PLAN).forEach { cancel(context, it) }
    }

    // ---------- Focus ----------

    fun scheduleFocusEnd(context: Context, atMillis: Long, label: String) =
        schedule(context, FOCUS_END, atMillis, "Session complete 🎉", "Nice work on $label. Come back to lock in what you learned.")

    fun scheduleBreakEnd(context: Context, atMillis: Long) =
        schedule(context, BREAK_END, atMillis, "Break's over", "Ready for another round? Your next session is one tap away.")

    /** Fires only if the student is still away 90 seconds after leaving mid-session. */
    fun scheduleComeBack(context: Context, remainingMillis: Long) {
        val minutes = (remainingMillis / 60_000).coerceAtLeast(1)
        schedule(context, COME_BACK, System.currentTimeMillis() + 90_000, "Your focus session is still running",
            "$minutes min left. If you locked your phone to focus, ignore this. Otherwise — come back and finish strong.")
    }

    fun cancelComeBack(context: Context) = cancel(context, COME_BACK)

    fun cancelFocus(context: Context) = listOf(FOCUS_END, BREAK_END, COME_BACK).forEach { cancel(context, it) }

    // ---------- Helpers ----------

    private fun at(day: LocalDate, hour: Int): Long =
        day.atTime(LocalTime.of(hour.coerceIn(0, 23), 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun pending(context: Context, key: String, title: String, body: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_TITLE, title)
            .putExtra(ReminderReceiver.EXTRA_BODY, body)
            .putExtra(ReminderReceiver.EXTRA_ID, key.hashCode())
        return PendingIntent.getBroadcast(context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun schedule(context: Context, key: String, atMillis: Long, title: String, body: String) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        // Inexact alarms need no special permission; a minute of drift is fine for reminders.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(context, key, title, body))
    }

    private fun cancel(context: Context, key: String) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(pending(context, key, "", ""))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_ID = "id"
    }

    @SuppressLint("MissingPermission") // checked by Reminders.canNotify above
    override fun onReceive(context: Context, intent: Intent) {
        if (!Reminders.canNotify(context)) return
        Reminders.ensureChannel(context)
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "avenor_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE) ?: "Avenor")
            .setContentText(intent.getStringExtra(EXTRA_BODY) ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(intent.getStringExtra(EXTRA_BODY) ?: ""))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(intent.getIntExtra(EXTRA_ID, 1), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; nothing to do.
        }
    }
}

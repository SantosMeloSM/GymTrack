package com.gymtrack.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymtrack.app.GymTrackApp
import com.gymtrack.app.MainActivity
import com.gymtrack.app.R
import com.gymtrack.app.data.db.ReminderEntity
import com.gymtrack.app.data.db.SessionEntity
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object Channels {
    const val REST = "rest_timer"
    const val WORKOUT = "active_workout"
    const val REMINDERS = "reminders"

    fun create(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(REST, "Fim do descanso", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisa quando o tempo de descanso termina"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
        })
        nm.createNotificationChannel(NotificationChannel(WORKOUT, "Treino em andamento", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Mostra o cronômetro do treino ativo"
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(REMINDERS, "Lembretes de treino", NotificationManager.IMPORTANCE_DEFAULT))
    }
}

object NotificationIds {
    const val REST = 1001
    const val WORKOUT = 1002
    const val REMINDER_BASE = 2000
}

fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppIntent(): PendingIntent = PendingIntent.getActivity(
    this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

fun Context.vibrateRestDone() {
    val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
    vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
}

/** Tracks whether the UI is visible, so alarms do not post notifications over the open app. */
object AppVisibility { @Volatile var foreground: Boolean = false }

object RestTimerScheduler {
    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1, Intent(context, RestAlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Schedules the "rest finished" alarm. Uses an exact alarm when allowed, inexact otherwise. */
    fun schedule(context: Context, endsAtWallClock: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val trigger = SystemClock.elapsedRealtime() + (endsAtWallClock - System.currentTimeMillis()).coerceAtLeast(0)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi(context))
        else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pi(context))
        NotificationManagerCompat.from(context).cancel(NotificationIds.REST)
    }
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as GymTrackApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = app.container.settings.current()
                val session = app.container.workouts.getActiveSession()
                if (session?.restEndsAt == null) return@launch
                if (settings.vibrate) context.vibrateRestDone()
                if (!AppVisibility.foreground && settings.restNotifications && context.canPostNotifications()) {
                    val n = NotificationCompat.Builder(context, Channels.REST)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Descanso finalizado")
                        .setContentText(session.restExerciseName?.let { "Próxima série: $it" } ?: "Hora da próxima série")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setContentIntent(context.openAppIntent())
                        .build()
                    @Suppress("MissingPermission")
                    NotificationManagerCompat.from(context).notify(NotificationIds.REST, n)
                }
                app.container.workouts.clearRest(session.id)
                WorkoutNotification.refresh(context)
            } finally { pending.finish() }
        }
    }
}

/** Low-priority ongoing notification with a live chronometer while a workout runs in the background. */
object WorkoutNotification {
    fun show(context: Context, s: SessionEntity) {
        if (!context.canPostNotifications()) return
        val now = System.currentTimeMillis()
        val resting = s.restEndsAt != null && s.restEndsAt > now && s.pausedAt == null
        val b = NotificationCompat.Builder(context, Channels.WORKOUT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.name)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(context.openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        when {
            s.pausedAt != null -> b.setContentText("Treino pausado").setShowWhen(false)
            resting -> b.setContentText("Descansando${s.restExerciseName?.let { " · $it" } ?: ""}")
                .setWhen(s.restEndsAt!!).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
            else -> b.setContentText("Treino em andamento").setWhen(now - s.elapsedMs(now)).setUsesChronometer(true).setShowWhen(true)
        }
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(NotificationIds.WORKOUT, b.build())
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NotificationIds.WORKOUT)

    /** Re-posts (when app is in background) or clears the ongoing notification. */
    suspend fun refresh(context: Context) {
        val app = context.applicationContext as GymTrackApp
        val s = app.container.workouts.getActiveSession()
        if (s == null || AppVisibility.foreground) cancel(context) else show(context, s)
    }
}

object ReminderScheduler {
    private fun pi(context: Context, id: Long) = PendingIntent.getBroadcast(
        context, (NotificationIds.REMINDER_BASE + id).toInt(),
        Intent(context, ReminderReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun nextTrigger(r: ReminderEntity, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val day = DayOfWeek.of(r.dayOfWeek.coerceIn(1, 7))
        var candidate = now.with(TemporalAdjusters.nextOrSame(day)).withHour(r.hour).withMinute(r.minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
        return candidate
    }

    fun schedule(context: Context, r: ReminderEntity) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (!r.enabled) { am.cancel(pi(context, r.id)); return }
        val at = nextTrigger(r).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Reminders do not need second precision: an inexact, doze-friendly alarm is enough.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(context, r.id))
    }

    fun cancel(context: Context, id: Long) = context.getSystemService(AlarmManager::class.java).cancel(pi(context, id))

    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext as GymTrackApp
        app.container.reminders.getAll().forEach { schedule(context, it) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1)
        val app = context.applicationContext as GymTrackApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val r = app.container.reminders.get(id) ?: return@launch
                if (!r.enabled) return@launch
                if (context.canPostNotifications()) {
                    val n = NotificationCompat.Builder(context, Channels.REMINDERS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Hora de treinar")
                        .setContentText(r.label.ifBlank { "Seu treino está esperando por você." })
                        .setAutoCancel(true)
                        .setContentIntent(context.openAppIntent())
                        .build()
                    @Suppress("MissingPermission")
                    NotificationManagerCompat.from(context).notify((NotificationIds.REMINDER_BASE + id).toInt(), n)
                }
                ReminderScheduler.schedule(context, r)
            } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(context)
                val app = context.applicationContext as GymTrackApp
                app.container.workouts.getActiveSession()?.restEndsAt?.let { end ->
                    if (end > System.currentTimeMillis()) RestTimerScheduler.schedule(context, end)
                }
            } finally { pending.finish() }
        }
    }
}

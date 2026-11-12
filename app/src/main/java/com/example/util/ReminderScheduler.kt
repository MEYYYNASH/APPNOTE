package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NotesRepository
import com.example.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val noteId = intent?.getStringExtra(EXTRA_NOTE_ID) ?: return
        val title = intent.getStringExtra(EXTRA_NOTE_TITLE).orEmpty().ifBlank { "Note Reminder" }
        val body = intent.getStringExtra(EXTRA_NOTE_BODY).orEmpty().ifBlank { "Tap to open your note in GlassNotes." }
        ReminderScheduler.showReminderNotification(context, noteId, title, body)
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_BODY = "extra_note_body"
    }
}

object ReminderScheduler {
    private const val CHANNEL_ID = "glassnotes_reminders_channel"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminders for your GlassNotes"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun isInDoNotDisturbWindow(settings: AppSettings, isPinnedNote: Boolean = false): Boolean {
        if (!settings.dndEnabled) return false
        if (isPinnedNote && settings.dndAllowPinnedBypass) return false
        val cal = Calendar.getInstance()
        val curHour = cal.get(Calendar.HOUR_OF_DAY)
        val start = settings.dndStartHour.coerceIn(0, 23)
        val end = settings.dndEndHour.coerceIn(0, 23)
        return if (start <= end) {
            curHour in start until end
        } else {
            curHour >= start || curHour < end
        }
    }

    fun scheduleNoteReminder(
        context: Context,
        noteId: String,
        title: String,
        body: String,
        triggerAtMillis: Long
    ) {
        ensureNotificationChannel(context)
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_ID, noteId)
                putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_TITLE, title.ifBlank { "Untitled Note" })
                putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_BODY, body.take(120))
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                noteId.hashCode(),
                intent,
                flags
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }

    fun cancelNoteReminder(context: Context, noteId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                noteId.hashCode(),
                intent,
                flags
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun showReminderNotification(
        context: Context,
        noteId: String,
        title: String,
        body: String
    ) {
        ensureNotificationChannel(context)
        try {
            val repo = NotesRepository(context)
            val settings = repo.loadSettings()
            val note = repo.loadNotes().find { it.id == noteId }
            val inDnd = isInDoNotDisturbWindow(settings, note?.isPinned == true)

            if (!inDnd && settings.reminderSoundEnabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    AudioHelper.playReminderPreviewOrAlert(
                        context = context.applicationContext,
                        settings = settings,
                        overrideStyleId = if (settings.differentSoundsPerReminder) note?.reminderSoundStyle else null,
                        overrideAlertMode = if (settings.differentSoundsPerReminder) note?.reminderAlertMode else null,
                        overrideVibrationPattern = if (settings.differentSoundsPerReminder) note?.reminderVibrationPattern else null,
                        overrideVolumePercent = if (settings.differentSoundsPerReminder && (note?.reminderVolume ?: -1) >= 0) note?.reminderVolume else null
                    )
                }
            }
        } catch (_: Exception) {
            try {
                AudioHelper.ensureMediaVolumeAudible(context)
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            } catch (_: Exception) {
            }
        }

        try {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpen = PendingIntent.getActivity(
                context,
                noteId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Reminder: $title")
                .setContentText(body.ifBlank { "Scheduled note reminder" })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingOpen)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(noteId.hashCode(), notification)
        } catch (_: Exception) {
        }
    }

    fun showNewVersionNotification(
        context: Context,
        newVersion: String,
        changelog: String
    ) {
        ensureNotificationChannel(context)
        try {
            val repo = NotesRepository(context)
            val settings = repo.loadSettings()
            if (!isInDoNotDisturbWindow(settings, false) && settings.reminderSoundEnabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    AudioHelper.playReminderPreviewOrAlert(
                        context = context.applicationContext,
                        settings = settings,
                        overrideStyleId = settings.customNotificationSoundStyle
                    )
                }
            }
        } catch (_: Exception) {
            try {
                AudioHelper.ensureMediaVolumeAudible(context)
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 380)
            } catch (_: Exception) {
            }
        }

        try {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpen = PendingIntent.getActivity(
                context,
                newVersion.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New Version v$newVersion Available")
                .setContentText(changelog.ifBlank { "GlassNotes v$newVersion is ready to update." })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingOpen)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(("version_$newVersion").hashCode(), notification)
        } catch (_: Exception) {
        }
    }

    fun formatReminderTime(epochMillis: Long): String {
        val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return fmt.format(Date(epochMillis))
    }
}

package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.NotesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.toEntity
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = NotesRepository(applicationContext)
            val notes = repo.loadNotes()
            val dao = AppDatabase.getInstance(applicationContext).noteDao()
            dao.replaceAll(notes.map { it.toEntity() })

            val now = System.currentTimeMillis()
            notes.filter {
                !it.isTrashed &&
                    !it.reminderFired &&
                    it.reminderAt != null &&
                    it.reminderAt > now
            }.forEach { note ->
                note.reminderAt?.let { triggerAt ->
                    ReminderScheduler.scheduleNoteReminder(
                        context = applicationContext,
                        noteId = note.id,
                        title = note.title.ifBlank { "Untitled Note" },
                        body = note.body,
                        triggerAtMillis = triggerAt
                    )
                }
            }

            pushGlanceRevisionUpdate(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "glassnotes_widget_periodic_sync"
        private const val UNIQUE_ONESHOT_NAME = "glassnotes_widget_immediate_sync"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Int) {
            val wm = WorkManager.getInstance(context.applicationContext)
            if (intervalMinutes <= 0) {
                wm.cancelUniqueWork(UNIQUE_PERIODIC_NAME)
                return
            }
            val safeInterval = intervalMinutes.toLong().coerceAtLeast(15L)
            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(
                safeInterval,
                TimeUnit.MINUTES
            ).build()
            wm.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetSyncWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ONESHOT_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}

class BootWidgetRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            WidgetSyncWorker.enqueueImmediateSync(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val cfg = WidgetDataStore.getStudioConfig(context.applicationContext)
                if (cfg.refreshIntervalMinutes >= 15) {
                    WidgetSyncWorker.schedulePeriodicSync(
                        context.applicationContext,
                        cfg.refreshIntervalMinutes
                    )
                }
            }
        }
    }
}

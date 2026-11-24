package com.lduarte1311.calsyncalarm.engine

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lduarte1311.calsyncalarm.data.AppSettings
import java.util.concurrent.TimeUnit

/**
 * Worker do WorkManager para sincronização periódica de fundo e reforço do agendamento de alarmes.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = AppSettings(applicationContext)
        val activeCalendars = settings.getCalendars().filter { it.enabled }

        if (activeCalendars.isNotEmpty()) {
            AlarmScheduler.scheduleNextAlarm(applicationContext)
            settings.lastSyncTime = System.currentTimeMillis()
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "CalendarWake_PeriodicSync"

        /**
         * Enfileira a tarefa periódica no WorkManager (reforço a cada 6 horas).
         */
        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

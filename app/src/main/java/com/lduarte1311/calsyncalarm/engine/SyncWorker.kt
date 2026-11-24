package com.lduarte1311.calsyncalarm.engine

import android.content.Context
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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
            val readOk = AlarmScheduler.scheduleNextAlarm(applicationContext)
            if (readOk) settings.lastSyncTime = System.currentTimeMillis()
        }

        // O gatilho por alteração só dispara uma vez: voltar a registá-lo para a próxima alteração
        if (inputData.getBoolean(KEY_FROM_CHANGE, false)) {
            enqueueChangeObserver(applicationContext, rearm = true)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "CalSyncAlarm_PeriodicSync"
        private const val CHANGE_WORK_NAME = "CalSyncAlarm_CalendarChange"
        private const val KEY_FROM_CHANGE = "from_calendar_change"

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

        /**
         * Regista um trabalho que corre quando o calendário do sistema muda (evento criado, movido
         * ou apagado), para o alarme não ficar até 6 horas com dados antigos.
         * Com rearm = true (chamado pelo próprio worker) encadeia o seguinte; caso contrário só
         * regista se ainda não houver um pendente.
         */
        fun enqueueChangeObserver(context: Context, rearm: Boolean = false) {
            val constraints = Constraints.Builder()
                .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
                .setTriggerContentUpdateDelay(10, TimeUnit.SECONDS)
                .setTriggerContentMaxDelay(60, TimeUnit.SECONDS)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FROM_CHANGE to true))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                CHANGE_WORK_NAME,
                if (rearm) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}

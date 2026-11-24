package com.lduarte1311.calsyncalarm.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receptor de inicialização e alterações de sistema (Boot, fuso horário, atualização de pacote).
 * Compatível com Direct Boot (LOCKED_BOOT_COMPLETED).
 */
class InitReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Reagendar o próximo alarme no AlarmManager
                AlarmScheduler.scheduleNextAlarm(context)

                // Garantir agendamento periódico no WorkManager
                SyncWorker.enqueuePeriodicSync(context)
            }
        }
    }
}

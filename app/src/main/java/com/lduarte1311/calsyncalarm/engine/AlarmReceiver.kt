package com.lduarte1311.calsyncalarm.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lduarte1311.calsyncalarm.data.AppSettings
import com.lduarte1311.calsyncalarm.data.SnoozeInfo

/**
 * Receptor de mensagens de broadcast para disparos de alarmes e ações de utilizador nas notificações.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.lduarte1311.calsyncalarm.ACTION_TRIGGER_ALARM"
        const val ACTION_PRE_ALARM = "com.lduarte1311.calsyncalarm.ACTION_PRE_ALARM"
        const val ACTION_SKIP_ALARM = "com.lduarte1311.calsyncalarm.ACTION_SKIP_ALARM"
        const val ACTION_DISMISS_SNOOZE = "com.lduarte1311.calsyncalarm.ACTION_DISMISS_SNOOZE"
        const val ACTION_DISMISS_ALARM = "com.lduarte1311.calsyncalarm.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.lduarte1311.calsyncalarm.ACTION_SNOOZE_ALARM"
        const val ACTION_TEST_ALARM = "com.lduarte1311.calsyncalarm.ACTION_TEST_ALARM"

        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_START_TIME = "extra_event_start_time"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val settings = AppSettings(context)

        when (intent.action) {
            ACTION_TRIGGER_ALARM -> {
                val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarme de Calendário"

                // Se este alarme estava em Snooze, limpa o estado de snooze
                if (settings.snoozeInfo?.eventId == eventId) {
                    settings.snoozeInfo = null
                    NotificationHelper.cancelSnoozeNotification(context)
                }

                // Cancela eventual notificação pré-alarme que ainda esteja visível
                NotificationHelper.cancelPreAlarmNotification(context)

                // Iniciar o ForegroundService com o calendarId associado
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(AlarmService.EXTRA_CALENDAR_ID, calendarId)
                    putExtra(AlarmService.EXTRA_EVENT_ID, eventId)
                    putExtra(AlarmService.EXTRA_EVENT_START_TIME, eventStartTime)
                    putExtra(AlarmService.EXTRA_TITLE, title)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // Agendar já o alarme seguinte, para a cadeia não depender de desligar/adiar/timeout
                try {
                    AlarmScheduler.scheduleNextAlarm(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ACTION_PRE_ALARM -> {
                val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Evento com hora marcada"
                val alarmTime = intent.getLongExtra(EXTRA_ALARM_TIME, System.currentTimeMillis())

                NotificationHelper.showPreAlarmNotification(
                    context = context,
                    calendarId = calendarId,
                    eventId = eventId,
                    eventStartTime = eventStartTime,
                    title = title,
                    alarmTime = alarmTime
                )
            }

            ACTION_SKIP_ALARM -> {
                // Ação rápida: Saltar este alarme no respetivo calendário
                val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0L)

                if (calendarId != -1L && eventId != -1L && eventStartTime != 0L) {
                    val key = "${eventId}_${eventStartTime}"
                    settings.toggleOccurrence(calendarId, key, false)
                }

                NotificationHelper.cancelPreAlarmNotification(context)
                AlarmScheduler.scheduleNextAlarm(context)
            }

            ACTION_DISMISS_SNOOZE -> {
                // Desligar o Snooze em curso
                settings.snoozeInfo = null
                NotificationHelper.cancelSnoozeNotification(context)
                AlarmScheduler.scheduleNextAlarm(context)
            }

            ACTION_DISMISS_ALARM -> {
                // Desligar o alarme ativo
                val stopIntent = Intent(context, AlarmService::class.java)
                context.stopService(stopIntent)
                AlarmScheduler.scheduleNextAlarm(context)
            }

            ACTION_SNOOZE_ALARM -> {
                // Adiar o alarme utilizando a duração configurada no respetivo calendário
                val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarme"

                val stopIntent = Intent(context, AlarmService::class.java)
                context.stopService(stopIntent)

                val calConfig = if (calendarId != -1L) settings.getCalendar(calendarId) else null
                val snoozeMinutes = calConfig?.snoozeMinutes ?: settings.defaultSnoozeMinutes
                val snoozeMillis = snoozeMinutes * 60_000L
                val triggerTime = System.currentTimeMillis() + snoozeMillis

                settings.snoozeInfo = SnoozeInfo(
                    calendarId = calendarId,
                    eventId = eventId,
                    eventStartTime = eventStartTime,
                    title = title,
                    triggerTime = triggerTime
                )

                NotificationHelper.showSnoozeNotification(context, title, triggerTime)
                AlarmScheduler.scheduleNextAlarm(context)
            }

            ACTION_TEST_ALARM -> {
                // Disparo de teste
                val now = System.currentTimeMillis()
                val testServiceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(AlarmService.EXTRA_CALENDAR_ID, -1L)
                    putExtra(AlarmService.EXTRA_EVENT_ID, 999999L)
                    putExtra(AlarmService.EXTRA_EVENT_START_TIME, now + 5000L)
                    putExtra(AlarmService.EXTRA_TITLE, "Teste de Alarme")
                }
                ContextCompat.startForegroundService(context, testServiceIntent)
            }
        }
    }
}

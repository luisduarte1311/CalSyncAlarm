package com.lduarte1311.calsyncalarm.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lduarte1311.calsyncalarm.data.AlarmOccurrence
import com.lduarte1311.calsyncalarm.data.AppSettings
import com.lduarte1311.calsyncalarm.data.CalendarReader
import com.lduarte1311.calsyncalarm.ui.MainActivity

/**
 * Motor central de agendamento multi-calendário.
 * Implementa a regra estrita de "Fila Única no AlarmManager":
 * Junta os próximos 30 dias de todos os calendários ativos e agenda apenas 1 setAlarmClock()
 * (e 1 pré-alarme) para o próximo evento cronológico mais cedo entre todos.
 */
object AlarmScheduler {

    private const val REQUEST_CODE_ALARM = 100
    private const val REQUEST_CODE_PRE_ALARM = 200
    private const val REQUEST_CODE_SHOW_INTENT = 300

    /**
     * Recolhe as ocorrências ativas dos próximos 30 dias de todos os calendários ativos.
     */
    fun getAllUpcomingOccurrences(
        context: Context,
        now: Long = System.currentTimeMillis()
    ): List<AlarmOccurrence> {
        val settings = AppSettings(context)
        val activeCalendars = settings.getCalendars().filter { it.enabled }
        if (activeCalendars.isEmpty()) return emptyList()

        val allOccurrences = mutableListOf<AlarmOccurrence>()
        for (cal in activeCalendars) {
            val list = CalendarReader.loadUpcomingOccurrences(
                context = context,
                calendarId = cal.id,
                calendarDisplayName = cal.displayName,
                calendarColor = cal.color,
                offsetMinutes = cal.offsetMinutes,
                disabledKeys = cal.disabledKeys,
                now = now
            )
            allOccurrences.addAll(list)
        }
        return allOccurrences.sortedBy { it.alarmTime }
    }

    /**
     * Retorna a próxima ocorrência cronológica ativa entre todos os calendários.
     */
    fun getNextUpcomingAlarm(
        context: Context,
        now: Long = System.currentTimeMillis()
    ): AlarmOccurrence? {
        val list = getAllUpcomingOccurrences(context, now)
        return list.firstOrNull { it.isEnabled && it.alarmTime > now }
    }

    /**
     * Recalcula os eventos de todos os calendários ativos e agenda o próximo alarme no AlarmManager.
     */
    fun scheduleNextAlarm(context: Context) {
        val settings = AppSettings(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        // Limpeza de histórico de chaves desativadas com > 24h
        settings.cleanOldDisabledKeys(now)

        val activeCalendars = settings.getCalendars().filter { it.enabled }
        if (activeCalendars.isEmpty()) {
            cancelAll(context)
            return
        }

        val allOccurrences = getAllUpcomingOccurrences(context, now)
        val nextOccurrence = allOccurrences.firstOrNull { it.isEnabled && it.alarmTime > now }

        // Verificar se há um Snooze ativo no futuro
        val activeSnooze = settings.snoozeInfo?.let {
            if (it.triggerTime > now) it else {
                settings.snoozeInfo = null // Expirou
                NotificationHelper.cancelSnoozeNotification(context)
                null
            }
        }

        // Determinar o próximo alvo mais cedo entre Snooze e a ocorrência mais próxima
        val nextTarget: TargetAlarm? = when {
            activeSnooze != null && nextOccurrence != null -> {
                if (activeSnooze.triggerTime <= nextOccurrence.alarmTime) {
                    TargetAlarm(
                        calendarId = activeSnooze.calendarId,
                        eventId = activeSnooze.eventId,
                        eventStartTime = activeSnooze.eventStartTime,
                        title = activeSnooze.title,
                        triggerTime = activeSnooze.triggerTime,
                        isSnooze = true,
                        preAlarmMinutes = 0
                    )
                } else {
                    val calConfig = settings.getCalendar(nextOccurrence.calendarId)
                    TargetAlarm(
                        calendarId = nextOccurrence.calendarId,
                        eventId = nextOccurrence.eventId,
                        eventStartTime = nextOccurrence.eventStartTime,
                        title = nextOccurrence.title,
                        triggerTime = nextOccurrence.alarmTime,
                        isSnooze = false,
                        preAlarmMinutes = calConfig?.preAlarmMinutes ?: settings.defaultPreAlarmMinutes
                    )
                }
            }
            activeSnooze != null -> {
                TargetAlarm(
                    calendarId = activeSnooze.calendarId,
                    eventId = activeSnooze.eventId,
                    eventStartTime = activeSnooze.eventStartTime,
                    title = activeSnooze.title,
                    triggerTime = activeSnooze.triggerTime,
                    isSnooze = true,
                    preAlarmMinutes = 0
                )
            }
            nextOccurrence != null -> {
                val calConfig = settings.getCalendar(nextOccurrence.calendarId)
                TargetAlarm(
                    calendarId = nextOccurrence.calendarId,
                    eventId = nextOccurrence.eventId,
                    eventStartTime = nextOccurrence.eventStartTime,
                    title = nextOccurrence.title,
                    triggerTime = nextOccurrence.alarmTime,
                    isSnooze = false,
                    preAlarmMinutes = calConfig?.preAlarmMinutes ?: settings.defaultPreAlarmMinutes
                )
            }
            else -> null
        }

        if (nextTarget == null) {
            cancelAll(context)
            return
        }

        // 1. Agendar exatamente 1 setAlarmClock() no AlarmManager
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW_INTENT,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_CALENDAR_ID, nextTarget.calendarId)
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, nextTarget.eventId)
            putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, nextTarget.eventStartTime)
            putExtra(AlarmReceiver.EXTRA_TITLE, nextTarget.title)
        }
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextTarget.triggerTime, showPendingIntent),
                alarmPendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // 2. Agendar a notificação pré-alarme se configurada para o calendário do evento
        if (!nextTarget.isSnooze && nextTarget.preAlarmMinutes > 0) {
            val preAlarmTime = nextTarget.triggerTime - (nextTarget.preAlarmMinutes * 60_000L)
            val preAlarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_PRE_ALARM
                putExtra(AlarmReceiver.EXTRA_CALENDAR_ID, nextTarget.calendarId)
                putExtra(AlarmReceiver.EXTRA_EVENT_ID, nextTarget.eventId)
                putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, nextTarget.eventStartTime)
                putExtra(AlarmReceiver.EXTRA_TITLE, nextTarget.title)
                putExtra(AlarmReceiver.EXTRA_ALARM_TIME, nextTarget.triggerTime)
            }
            val preAlarmPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_PRE_ALARM,
                preAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (preAlarmTime > now) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        preAlarmTime,
                        preAlarmPendingIntent
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            } else if (now < nextTarget.triggerTime) {
                // Já dentro da janela de pré-alarme
                NotificationHelper.showPreAlarmNotification(
                    context = context,
                    calendarId = nextTarget.calendarId,
                    eventId = nextTarget.eventId,
                    eventStartTime = nextTarget.eventStartTime,
                    title = nextTarget.title,
                    alarmTime = nextTarget.triggerTime
                )
            }
        } else {
            cancelPreAlarm(context)
        }
    }

    /**
     * Cancela todos os alarmes e pré-alarmes pendentes no AlarmManager.
     */
    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            alarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent)
            alarmPendingIntent.cancel()
        }

        cancelPreAlarm(context)
        NotificationHelper.cancelPreAlarmNotification(context)
    }

    private fun cancelPreAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val preAlarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRE_ALARM
        }
        val preAlarmPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PRE_ALARM,
            preAlarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (preAlarmPendingIntent != null) {
            alarmManager.cancel(preAlarmPendingIntent)
            preAlarmPendingIntent.cancel()
        }
    }

    private data class TargetAlarm(
        val calendarId: Long,
        val eventId: Long,
        val eventStartTime: Long,
        val title: String,
        val triggerTime: Long,
        val isSnooze: Boolean,
        val preAlarmMinutes: Int
    )
}

package com.lduarte1311.calsyncalarm.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lduarte1311.calsyncalarm.R
import com.lduarte1311.calsyncalarm.ui.AlarmActivity
import com.lduarte1311.calsyncalarm.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor centralizado de canais e notificações do sistema.
 */
object NotificationHelper {

    const val CHANNEL_ALARM_ID = "channel_calendar_wake_alarm"
    const val CHANNEL_PRE_ALARM_ID = "channel_calendar_wake_pre_alarm"
    const val CHANNEL_SNOOZE_ID = "channel_calendar_wake_snooze"
    const val CHANNEL_MISSED_ID = "channel_calendar_wake_missed"

    const val NOTIFICATION_ALARM_ID = 1001
    const val NOTIFICATION_PRE_ALARM_ID = 1002
    const val NOTIFICATION_SNOOZE_ID = 1003
    const val NOTIFICATION_MISSED_ID = 1004

    /**
     * Regista todos os canais de notificação necessários no NotificationManager.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal 1: Alarme sonoro principal (Alta prioridade)
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Alarme Ativo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Disparo sonoro e visual do alarme de eventos com hora marcada"
                setSound(null, null) // O áudio é controlado diretamente pelo MediaPlayer do AlarmService
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Canal 2: Notificações pré-alarme (Informativas)
            val preAlarmChannel = NotificationChannel(
                CHANNEL_PRE_ALARM_ID,
                "Avisos Pré-Alarme",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos prévios que permitem saltar o próximo alarme com antecedência"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Canal 3: Notificações de Snooze
            val snoozeChannel = NotificationChannel(
                CHANNEL_SNOOZE_ID,
                "Alarmes em Snooze",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aviso persistente durante o período de adiamento do alarme"
                setShowBadge(false)
            }

            // Canal 4: Alarmes Perdidos
            val missedChannel = NotificationChannel(
                CHANNEL_MISSED_ID,
                "Alarmes Perdidos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aviso quando um alarme atingiu o limite de segurança de 10 minutos"
            }

            manager.createNotificationChannels(listOf(alarmChannel, preAlarmChannel, snoozeChannel, missedChannel))
        }
    }

    /**
     * Constrói a notificação de Foreground para o AlarmService com FullScreenIntent e botões de ação.
     */
    fun buildAlarmNotification(
        context: Context,
        calendarId: Long,
        eventId: Long,
        eventStartTime: Long,
        title: String
    ): Notification {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val eventHour = timeFormat.format(Date(eventStartTime))

        // Intent de ecrã total para abrir o AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_CALENDAR_ID, calendarId)
            putExtra(AlarmActivity.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmActivity.EXTRA_EVENT_START_TIME, eventStartTime)
            putExtra(AlarmActivity.EXTRA_TITLE, title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação Snooze
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmReceiver.EXTRA_CALENDAR_ID, calendarId)
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, eventStartTime)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação Desligar
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarme: $title")
            .setContentText("Evento marcado para as $eventHour")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desligar", dismissPendingIntent)
            .build()
    }

    /**
     * Exibe a notificação persistente de Snooze ativo.
     */
    fun showSnoozeNotification(context: Context, title: String, triggerTime: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val wakeHour = timeFormat.format(Date(triggerTime))

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_SNOOZE
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            5,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SNOOZE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Em Snooze — $title")
            .setContentText("Toca novamente às $wakeHour")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desligar", dismissPendingIntent)
            .build()

        manager.notify(NOTIFICATION_SNOOZE_ID, notification)
    }

    /**
     * Cancela a notificação de Snooze.
     */
    fun cancelSnoozeNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_SNOOZE_ID)
    }

    /**
     * Exibe notificação pré-alarme com botão "Saltar este alarme".
     */
    fun showPreAlarmNotification(
        context: Context,
        calendarId: Long,
        eventId: Long,
        eventStartTime: Long,
        title: String,
        alarmTime: Long
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val alarmHour = timeFormat.format(Date(alarmTime))

        val skipIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SKIP_ALARM
            putExtra(AlarmReceiver.EXTRA_CALENDAR_ID, calendarId)
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, eventStartTime)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            6,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PRE_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Próximo alarme às $alarmHour")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_next, "Saltar este alarme", skipPendingIntent)
            .build()

        manager.notify(NOTIFICATION_PRE_ALARM_ID, notification)
    }

    /**
     * Cancela notificação pré-alarme.
     */
    fun cancelPreAlarmNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_PRE_ALARM_ID)
    }

    /**
     * Emite notificação silenciosa de alarme perdido (timeout de 10 minutos).
     */
    fun showMissedAlarmNotification(context: Context, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarme perdido")
            .setContentText("O alarme para \"$title\" tocou durante 10 minutos sem resposta.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_MISSED_ID, notification)
    }
}
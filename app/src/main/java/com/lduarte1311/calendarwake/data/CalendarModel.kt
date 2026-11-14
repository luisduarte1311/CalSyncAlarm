package com.lduarte1311.calendarwake.data

/**
 * Representa um calendário do sistema (Google, Samsung, Local, etc.).
 */
data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int
)

/**
 * Configuração isolada de um calendário associado no Calendar Wake.
 */
data class CalendarConfig(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val enabled: Boolean = true,
    val offsetMinutes: Int = 90,
    val soundUri: String? = null,
    val volumePercent: Int = 90,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val preAlarmMinutes: Int = 120,
    val disabledKeys: Set<String> = emptySet()
)

/**
 * Representa uma ocorrência de evento com hora marcada e o respetivo alarme calculado.
 */
data class AlarmOccurrence(
    val calendarId: Long,
    val calendarDisplayName: String,
    val calendarColor: Int,
    val eventId: Long,
    val instanceId: Long,
    val title: String,
    val eventStartTime: Long,
    val eventEndTime: Long,
    val alarmTime: Long,
    val isEnabled: Boolean
) {
    /**
     * Chave única por ocorrência no âmbito do calendário: EVENT_ID + BEGIN.
     */
    val uniqueKey: String
        get() = "${eventId}_${eventStartTime}"
}

/**
 * Dados de um alarme em estado de Snooze ativo.
 */
data class SnoozeInfo(
    val calendarId: Long,
    val eventId: Long,
    val eventStartTime: Long,
    val title: String,
    val triggerTime: Long
)

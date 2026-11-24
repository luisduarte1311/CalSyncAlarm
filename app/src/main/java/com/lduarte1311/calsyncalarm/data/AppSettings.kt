package com.lduarte1311.calsyncalarm.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestor de definições e persistência da aplicação utilizando SharedPreferences e JSON.
 * Suporta múltiplos calendários simultâneos, cada um com as suas regras isoladas.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "calsyncalarm_prefs"

        // Chaves das Definições Globais (Valores padrão para novos calendários)
        private const val KEY_DEFAULT_OFFSET_MINUTES = "default_offset_minutes"
        private const val KEY_DEFAULT_VOLUME_PERCENT = "default_volume_percent"
        private const val KEY_DEFAULT_VIBRATE = "default_vibrate"
        private const val KEY_DEFAULT_SNOOZE_MINUTES = "default_snooze_minutes"
        private const val KEY_DEFAULT_PRE_ALARM_MINUTES = "default_pre_alarm_minutes"
        private const val KEY_DEFAULT_ALARM_SOUND_URI = "default_alarm_sound_uri"

        private const val KEY_CALENDARS_JSON = "configured_calendars_json"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_SNOOZE_JSON = "snooze_json"
        private const val KEY_LAST_PRE_ALARM = "last_pre_alarm_key"
        private const val KEY_NO_ALARM_NOTIFIED = "no_alarm_notified_keys"

        // Chaves legadas da V1 para migração automática
        private const val LEGACY_KEY_CALENDAR_ID = "selected_calendar_id"
        private const val LEGACY_KEY_CALENDAR_NAME = "calendar_display_name"
        private const val LEGACY_KEY_CALENDAR_ACCOUNT = "calendar_account_name"
        private const val LEGACY_KEY_DISABLED_KEYS = "disabled_keys"

        // Valores pré-definidos sensatos
        const val DEFAULT_OFFSET_MINUTES = 90 // 1h 30m
        const val DEFAULT_VOLUME_PERCENT = 90 // 90%
        const val DEFAULT_VIBRATE = true
        const val DEFAULT_SNOOZE_MINUTES = 10 // 10 min
        const val DEFAULT_PRE_ALARM_MINUTES = 120 // 2 horas (0 = Desligado)
    }

    init {
        migrateLegacyV1IfNeeded()
    }

    /* ---------------------------------------------------------
       Definições Globais (Padrões para novos calendários)
       --------------------------------------------------------- */

    var defaultOffsetMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_OFFSET_MINUTES, DEFAULT_OFFSET_MINUTES)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_OFFSET_MINUTES, value).apply()

    var defaultVolumePercent: Int
        get() = prefs.getInt(KEY_DEFAULT_VOLUME_PERCENT, DEFAULT_VOLUME_PERCENT)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_VOLUME_PERCENT, value.coerceIn(0, 100)).apply()

    var defaultVibrate: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_VIBRATE, DEFAULT_VIBRATE)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_VIBRATE, value).apply()

    var defaultSnoozeMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_SNOOZE_MINUTES, value).apply()

    var defaultPreAlarmMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_PRE_ALARM_MINUTES, DEFAULT_PRE_ALARM_MINUTES)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_PRE_ALARM_MINUTES, value).apply()

    var defaultAlarmSoundUri: String?
        get() = prefs.getString(KEY_DEFAULT_ALARM_SOUND_URI, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_ALARM_SOUND_URI, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    /** Chave do último pré-alarme mostrado, para não o repetir (ver NotificationHelper.preAlarmKey). */
    var lastPreAlarmKey: String?
        get() = prefs.getString(KEY_LAST_PRE_ALARM, null)
        set(value) = prefs.edit().putString(KEY_LAST_PRE_ALARM, value).apply()

    /** Chaves (eventId_início) dos eventos já avisados de que ficaram sem alarme. */
    val noAlarmTrackingStarted: Boolean
        get() = prefs.contains(KEY_NO_ALARM_NOTIFIED)

    var noAlarmNotifiedKeys: Set<String>
        get() = prefs.getStringSet(KEY_NO_ALARM_NOTIFIED, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_NO_ALARM_NOTIFIED, value).apply()

    /* ---------------------------------------------------------
       Gestão de Múltiplos Calendários com Regras Isoladas
       --------------------------------------------------------- */

    /**
     * Retorna a lista de todos os calendários configurados.
     */
    fun getCalendars(): List<CalendarConfig> {
        val jsonStr = prefs.getString(KEY_CALENDARS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CalendarConfig>()
            for (i in 0 until array.length()) {
                // Um registo estragado é ignorado sozinho, sem deitar fora os restantes calendários
                try {
                    list.add(parseCalendar(array.getJSONObject(i)))
                } catch (e: Exception) {
                    Log.e("CalSyncAlarm", "Erro em getCalendars", e)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseCalendar(obj: JSONObject): CalendarConfig {
        val disabledArray = obj.optJSONArray("disabledKeys")
        val disabledSet = mutableSetOf<String>()
        if (disabledArray != null) {
            for (j in 0 until disabledArray.length()) {
                disabledSet.add(disabledArray.getString(j))
            }
        }

        return CalendarConfig(
            id = obj.getLong("id"),
            displayName = obj.getString("displayName"),
            accountName = obj.optString("accountName", ""),
            accountType = obj.optString("accountType", "Local"),
            color = obj.optInt("color", 0),
            enabled = obj.optBoolean("enabled", true),
            offsetMinutes = obj.optInt("offsetMinutes", defaultOffsetMinutes),
            soundUri = if (obj.has("soundUri") && !obj.isNull("soundUri")) obj.getString("soundUri") else null,
            volumePercent = obj.optInt("volumePercent", defaultVolumePercent),
            vibrate = obj.optBoolean("vibrate", defaultVibrate),
            snoozeMinutes = obj.optInt("snoozeMinutes", defaultSnoozeMinutes),
            preAlarmMinutes = obj.optInt("preAlarmMinutes", defaultPreAlarmMinutes),
            disabledKeys = disabledSet
        )
    }

    /**
     * Retorna a configuração de um calendário específico pelo ID.
     */
    fun getCalendar(id: Long): CalendarConfig? {
        return getCalendars().firstOrNull { it.id == id }
    }

    /**
     * Guarda ou atualiza a configuração de um calendário isolado.
     */
    fun saveCalendar(config: CalendarConfig) {
        val current = getCalendars().toMutableList()
        val index = current.indexOfFirst { it.id == config.id }
        if (index != -1) {
            current[index] = config
        } else {
            current.add(config)
        }
        persistCalendars(current)
    }

    /**
     * Remove um calendário configurado.
     */
    fun removeCalendar(id: Long) {
        val current = getCalendars().filterNot { it.id == id }
        persistCalendars(current)
    }

    /**
     * Alterna o estado ON/OFF de um calendário completo.
     */
    fun toggleCalendar(id: Long, enabled: Boolean) {
        val current = getCalendars().map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        persistCalendars(current)
    }

    /**
     * Alterna o estado de uma ocorrência individual num calendário específico.
     */
    fun toggleOccurrence(calendarId: Long, uniqueKey: String, isEnabled: Boolean) {
        val current = getCalendars().map { cal ->
            if (cal.id == calendarId) {
                val newDisabled = cal.disabledKeys.toMutableSet()
                if (isEnabled) {
                    newDisabled.remove(uniqueKey)
                } else {
                    newDisabled.add(uniqueKey)
                }
                cal.copy(disabledKeys = newDisabled)
            } else cal
        }
        persistCalendars(current)
    }

    private fun persistCalendars(list: List<CalendarConfig>) {
        val array = JSONArray()
        for (cal in list) {
            val obj = JSONObject().apply {
                put("id", cal.id)
                put("displayName", cal.displayName)
                put("accountName", cal.accountName)
                put("accountType", cal.accountType)
                put("color", cal.color)
                put("enabled", cal.enabled)
                put("offsetMinutes", cal.offsetMinutes)
                put("soundUri", cal.soundUri ?: JSONObject.NULL)
                put("volumePercent", cal.volumePercent)
                put("vibrate", cal.vibrate)
                put("snoozeMinutes", cal.snoozeMinutes)
                put("preAlarmMinutes", cal.preAlarmMinutes)

                val disabledArr = JSONArray()
                for (key in cal.disabledKeys) {
                    disabledArr.put(key)
                }
                put("disabledKeys", disabledArr)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CALENDARS_JSON, array.toString()).apply()
    }

    /* ---------------------------------------------------------
       Gestão de Snooze
       --------------------------------------------------------- */

    var snoozeInfo: SnoozeInfo?
        get() {
            val jsonStr = prefs.getString(KEY_SNOOZE_JSON, null) ?: return null
            return try {
                val json = JSONObject(jsonStr)
                SnoozeInfo(
                    calendarId = json.optLong("calendarId", -1L),
                    eventId = json.getLong("eventId"),
                    eventStartTime = json.getLong("eventStartTime"),
                    title = json.getString("title"),
                    triggerTime = json.getLong("triggerTime")
                )
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_SNOOZE_JSON)
                } else {
                    val json = JSONObject().apply {
                        put("calendarId", value.calendarId)
                        put("eventId", value.eventId)
                        put("eventStartTime", value.eventStartTime)
                        put("title", value.title)
                        put("triggerTime", value.triggerTime)
                    }
                    putString(KEY_SNOOZE_JSON, json.toString())
                }
                apply()
            }
        }

    /**
     * Limpeza automática de registos com mais de 24h no passado em todos os calendários.
     */
    fun cleanOldDisabledKeys(now: Long = System.currentTimeMillis()) {
        val twentyFourHoursAgo = now - 24 * 60 * 60 * 1000L
        val current = getCalendars()
        var changed = false

        val updated = current.map { cal ->
            val cleaned = cal.disabledKeys.filter { key ->
                val parts = key.split("_")
                if (parts.size >= 2) {
                    val begin = parts[1].toLongOrNull() ?: 0L
                    begin >= twentyFourHoursAgo
                } else false
            }.toSet()

            if (cleaned.size != cal.disabledKeys.size) {
                changed = true
                cal.copy(disabledKeys = cleaned)
            } else cal
        }

        if (changed) {
            persistCalendars(updated)
        }
    }

    /* ---------------------------------------------------------
       Migração suave de V1 (Mono-calendário) para V2 (Multi)
       --------------------------------------------------------- */
    private fun migrateLegacyV1IfNeeded() {
        if (!prefs.contains(KEY_CALENDARS_JSON) && prefs.contains(LEGACY_KEY_CALENDAR_ID)) {
            val legacyId = prefs.getLong(LEGACY_KEY_CALENDAR_ID, -1L)
            if (legacyId != -1L) {
                val name = prefs.getString(LEGACY_KEY_CALENDAR_NAME, "Calendário") ?: "Calendário"
                val account = prefs.getString(LEGACY_KEY_CALENDAR_ACCOUNT, "") ?: ""
                val disabled = prefs.getStringSet(LEGACY_KEY_DISABLED_KEYS, emptySet()) ?: emptySet()

                val config = CalendarConfig(
                    id = legacyId,
                    displayName = name,
                    accountName = account,
                    accountType = "Local",
                    color = 0,
                    enabled = true,
                    offsetMinutes = defaultOffsetMinutes,
                    soundUri = defaultAlarmSoundUri,
                    volumePercent = defaultVolumePercent,
                    vibrate = defaultVibrate,
                    snoozeMinutes = defaultSnoozeMinutes,
                    preAlarmMinutes = defaultPreAlarmMinutes,
                    disabledKeys = disabled
                )
                persistCalendars(listOf(config))
            }
        }
    }
}

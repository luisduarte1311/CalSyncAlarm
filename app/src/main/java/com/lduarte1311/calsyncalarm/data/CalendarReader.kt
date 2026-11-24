package com.lduarte1311.calsyncalarm.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log

/**
 * Utilitário para leitura do CalendarContract do Android.
 * Consulta exclusivamente o calendário selecionado nos próximos 30 dias.
 */
object CalendarReader {

    /**
     * Devolve a lista de calendários presentes no dispositivo (Google, Samsung, Xiaomi, Local, etc.).
     */
    fun getAvailableCalendars(context: Context): List<CalendarAccount> {
        val list = mutableListOf<CalendarAccount>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val typeIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val name = it.getString(nameIdx) ?: "Sem Nome"
                    val account = it.getString(accountIdx) ?: ""
                    val type = it.getString(typeIdx) ?: "Local"
                    val color = if (colorIdx != -1) it.getInt(colorIdx) else 0

                    list.add(CalendarAccount(id, name, account, type, color))
                }
            }
        } catch (e: SecurityException) {
            // Permissão READ_CALENDAR ainda não concedida
        } catch (e: Exception) {
            Log.e("CalSyncAlarm", "Erro em getAvailableCalendars", e)
        }

        return list
    }

    /**
     * Consulta as instâncias de eventos nos próximos 30 dias para o calendário especificado.
     * Aplica os filtros: ALL_DAY == 0, STATUS != STATUS_CANCELED e alarmTime > agora.
     * Devolve null se a leitura falhar (sem permissão, erro do fornecedor ou cursor nulo);
     * lista vazia significa que a leitura correu bem e não há eventos.
     */
    fun loadUpcomingOccurrences(
        context: Context,
        calendarId: Long,
        calendarDisplayName: String = "",
        calendarColor: Int = 0,
        offsetMinutes: Int,
        disabledKeys: Set<String>,
        now: Long = System.currentTimeMillis()
    ): List<AlarmOccurrence>? {
        val occurrences = mutableListOf<AlarmOccurrence>()
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000L
        val windowEnd = now + thirtyDaysInMillis

        // O ContentProvider Instances requer o intervalo na URI
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, windowEnd)

        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS
        )

        // Filtro: apenas eventos do calendário escolhido, sem dia inteiro e não cancelados
        val selection = "${CalendarContract.Instances.CALENDAR_ID} = ? AND " +
                "${CalendarContract.Instances.ALL_DAY} = 0 AND " +
                "(${CalendarContract.Instances.STATUS} IS NULL OR ${CalendarContract.Instances.STATUS} != ?)"

        val selectionArgs = arrayOf(
            calendarId.toString(),
            CalendarContract.Instances.STATUS_CANCELED.toString()
        )

        val offsetMillis = offsetMinutes * 60_000L

        try {
            val cursor: Cursor = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            ) ?: return null

            cursor.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Instances._ID)
                val eventIdIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.END)

                while (it.moveToNext()) {
                    val instanceId = it.getLong(idIdx)
                    val eventId = it.getLong(eventIdIdx)
                    val rawTitle = it.getString(titleIdx)
                    val title = if (rawTitle.isNullOrBlank()) "Sem título" else rawTitle
                    val begin = it.getLong(beginIdx)
                    val end = it.getLong(endIdx)

                    val alarmTime = begin - offsetMillis

                    // Nunca agendar no passado
                    if (alarmTime > now) {
                        val key = "${eventId}_${begin}"
                        val isEnabled = !disabledKeys.contains(key)

                        occurrences.add(
                            AlarmOccurrence(
                                calendarId = calendarId,
                                calendarDisplayName = calendarDisplayName,
                                calendarColor = calendarColor,
                                eventId = eventId,
                                instanceId = instanceId,
                                title = title,
                                eventStartTime = begin,
                                eventEndTime = end,
                                alarmTime = alarmTime,
                                isEnabled = isEnabled
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permissão READ_CALENDAR ausente
            return null
        } catch (e: Exception) {
            Log.e("CalSyncAlarm", "Erro em loadUpcomingOccurrences", e)
            return null
        }

        return occurrences.sortedBy { it.alarmTime }
    }
}

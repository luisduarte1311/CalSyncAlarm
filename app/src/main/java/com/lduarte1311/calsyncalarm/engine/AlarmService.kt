package com.lduarte1311.calsyncalarm.engine

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.lduarte1311.calsyncalarm.data.AppSettings
import kotlin.math.roundToInt

/**
 * Serviço em primeiro plano para reprodução contínua do toque de alarme e vibração.
 * Aplica as regras de volume, som e vibração isoladas do calendário correspondente.
 */
class AlarmService : Service() {

    companion object {
        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_START_TIME = "extra_event_start_time"
        const val EXTRA_TITLE = "extra_title"

        private const val TIMEOUT_MILLIS = 10 * 60 * 1000L // 10 minutos de limite de segurança
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    private var originalVolume: Int? = null
    private var eventTitle: String = "Alarme"

    private val timeoutRunnable = Runnable {
        // Disparado se o alarme tocar durante 10 minutos sem intervenção do utilizador
        NotificationHelper.showMissedAlarmNotification(applicationContext, eventTitle)
        AlarmScheduler.scheduleNextAlarm(applicationContext)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CalSyncAlarm:AlarmServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(TIMEOUT_MILLIS + 10_000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0L)
        eventTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Alarme"

        val settings = AppSettings(this)
        val calConfig = if (calendarId != -1L) settings.getCalendar(calendarId) else null

        // 1. Iniciar notificação em Foreground
        val notification = NotificationHelper.buildAlarmNotification(
            context = this,
            calendarId = calendarId,
            eventId = eventId,
            eventStartTime = eventStartTime,
            title = eventTitle
        )

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ALARM_ID,
                notification,
                foregroundType
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Override temporário de volume para STREAM_ALARM com a percentagem isolada do calendário
        val targetVolumePercent = calConfig?.volumePercent ?: settings.defaultVolumePercent
        applyVolumeOverride(targetVolumePercent)

        // 3. Tocar som do alarme isolado do calendário
        val targetSoundUri = calConfig?.soundUri ?: settings.defaultAlarmSoundUri
        playAlarmSound(targetSoundUri)

        // 4. Iniciar vibração se ativa para este calendário
        val targetVibrate = calConfig?.vibrate ?: settings.defaultVibrate
        if (targetVibrate) {
            startVibration()
        }

        // 5. Agendar timeout de segurança de 10 minutos
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS)

        // Renovar o wake lock para acompanhar o novo timeout (não acumula: não é contado por referências)
        wakeLock?.acquire(TIMEOUT_MILLIS + 10_000L)

        return START_STICKY
    }

    private fun applyVolumeOverride(volumePercent: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            // Só guardar o volume original uma vez: num segundo alarme seguido, o volume atual já é o forçado
            if (originalVolume == null) {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            }

            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVol = (maxVol * (volumePercent / 100f)).roundToInt().coerceIn(1, maxVol)

            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalVolume?.let { savedVol ->
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, savedVol, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        originalVolume = null
    }

    private fun playAlarmSound(soundUriString: String?) {
        try {
            val uri: Uri = if (!soundUriString.isNullOrBlank()) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(applicationContext, fallbackUri)?.apply {
                    isLooping = true
                    start()
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 500, 800, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeoutRunnable)

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        restoreVolume()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

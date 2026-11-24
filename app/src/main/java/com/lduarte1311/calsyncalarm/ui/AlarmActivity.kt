package com.lduarte1311.calsyncalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lduarte1311.calsyncalarm.data.AppSettings
import com.lduarte1311.calsyncalarm.engine.AlarmReceiver
import com.lduarte1311.calsyncalarm.ui.theme.CalendarWakeTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ecrã de Despertar isolado.
 * Concebido para ativar o ecrã e ser visível sobre o ecrã de bloqueio sem desafios matemáticos.
 */
class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_START_TIME = "extra_event_start_time"
        const val EXTRA_TITLE = "extra_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarme"
        val settings = AppSettings(this)
        val calConfig = if (calendarId != -1L) settings.getCalendar(calendarId) else null
        val snoozeMinutes = calConfig?.snoozeMinutes ?: settings.defaultSnoozeMinutes

        setContent {
            CalendarWakeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmWakeContent(
                        eventId = eventId,
                        eventStartTime = eventStartTime,
                        title = title,
                        snoozeMinutes = snoozeMinutes,
                        onSnooze = {
                            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_SNOOZE_ALARM
                                putExtra(AlarmReceiver.EXTRA_CALENDAR_ID, calendarId)
                                putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
                                putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, eventStartTime)
                                putExtra(AlarmReceiver.EXTRA_TITLE, title)
                            }
                            sendBroadcast(snoozeIntent)
                            finish()
                        },
                        onDismiss = {
                            val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_DISMISS_ALARM
                            }
                            sendBroadcast(dismissIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmWakeContent(
    eventId: Long,
    eventStartTime: Long,
    title: String,
    snoozeMinutes: Int,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000L)
        }
    }

    val eventTimeString = if (eventStartTime > 0L) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(eventStartTime))
    } else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentTimeString,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (eventTimeString.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "O evento tem início às $eventTimeString",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Snooze, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Adiar ($snoozeMinutes min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Desligar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

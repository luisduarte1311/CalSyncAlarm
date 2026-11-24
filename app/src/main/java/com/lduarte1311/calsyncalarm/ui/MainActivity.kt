package com.lduarte1311.calsyncalarm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lduarte1311.calsyncalarm.data.AppSettings
import com.lduarte1311.calsyncalarm.engine.AlarmScheduler
import com.lduarte1311.calsyncalarm.engine.NotificationHelper
import com.lduarte1311.calsyncalarm.engine.SyncWorker
import com.lduarte1311.calsyncalarm.ui.theme.CalSyncAlarmTheme

/**
 * Ponto de entrada da aplicação.
 * Gere a navegação enxuta com suporte a pilha de ecrãs e BackHandler do Compose.
 */
class MainActivity : ComponentActivity() {

    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settings = AppSettings(this)

        // Inicializar canais de notificação e agendamento de sincronização
        NotificationHelper.createChannels(this)
        SyncWorker.enqueuePeriodicSync(this)
        SyncWorker.enqueueChangeObserver(this)

        setContent {
            CalSyncAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(settings = settings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (settings.getCalendars().any { it.enabled }) {
            AlarmScheduler.scheduleNextAlarm(this)
        }
    }
}

sealed class Screen {
    data object FirstLaunch : Screen()
    data object Main : Screen()
    data object GlobalSettings : Screen()
    data class CalendarSettings(val calendarId: Long) : Screen()
    data class CalendarEvents(val calendarId: Long) : Screen()
}

@Composable
fun AppNavigation(settings: AppSettings) {
    val backStack = remember {
        mutableStateListOf<Screen>(
            if (settings.getCalendars().isEmpty()) Screen.FirstLaunch else Screen.Main
        )
    }

    val currentScreen = backStack.lastOrNull() ?: Screen.Main

    // BackHandler do Compose sempre que não estiver no ecrã principal
    BackHandler(enabled = backStack.size > 1 && currentScreen !is Screen.Main) {
        backStack.removeAt(backStack.size - 1)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        } else {
            backStack.clear()
            backStack.add(Screen.Main)
        }
    }

    when (val screen = currentScreen) {
        Screen.FirstLaunch -> {
            FirstLaunchScreen(
                onCalendarSelected = {
                    backStack.clear()
                    backStack.add(Screen.Main)
                }
            )
        }

        Screen.Main -> {
            MainScreen(
                onNavigateToGlobalSettings = {
                    backStack.add(Screen.GlobalSettings)
                },
                onNavigateToCalendarSettings = { calId ->
                    backStack.add(Screen.CalendarSettings(calId))
                },
                onNavigateToCalendarEvents = { calId ->
                    backStack.add(Screen.CalendarEvents(calId))
                }
            )
        }

        Screen.GlobalSettings -> {
            SettingsScreen(
                calendarId = null,
                onNavigateBack = { navigateBack() }
            )
        }

        is Screen.CalendarSettings -> {
            SettingsScreen(
                calendarId = screen.calendarId,
                onNavigateBack = { navigateBack() },
                onCalendarRemoved = {
                    backStack.clear()
                    backStack.add(Screen.Main)
                }
            )
        }

        is Screen.CalendarEvents -> {
            CalendarEventsScreen(
                calendarId = screen.calendarId,
                onNavigateBack = { navigateBack() },
                onNavigateToSettings = { calId ->
                    backStack.add(Screen.CalendarSettings(calId))
                }
            )
        }
    }
}

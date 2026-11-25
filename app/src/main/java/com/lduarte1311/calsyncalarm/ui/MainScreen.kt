package com.lduarte1311.calsyncalarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lduarte1311.calsyncalarm.data.AlarmOccurrence
import com.lduarte1311.calsyncalarm.data.AppSettings
import com.lduarte1311.calsyncalarm.data.CalendarAccount
import com.lduarte1311.calsyncalarm.data.CalendarConfig
import com.lduarte1311.calsyncalarm.data.CalendarReader
import com.lduarte1311.calsyncalarm.engine.AlarmReceiver
import com.lduarte1311.calsyncalarm.engine.AlarmScheduler
import com.lduarte1311.calsyncalarm.engine.NotificationHelper
import com.lduarte1311.calsyncalarm.ui.components.PermissionBanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI Principal — Caminho B.
 * - Barra superior: + (adicionar calendário), ▶ (testar), ↻ (atualizar todos), ⚙ (Definições Globais).
 * - Cartão de Próximo Alarme Global e Cartão de Snooze ativo.
 * - Lista dos calendários configurados com switch isolado e botão ⚙ para definições do calendário.
 * - Toque no cartão abre os 30 dias desse calendário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToGlobalSettings: () -> Unit,
    onNavigateToCalendarSettings: (Long) -> Unit,
    onNavigateToCalendarEvents: (Long) -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var configuredCalendars by remember { mutableStateOf(settings.getCalendars()) }
    var nextUpcomingAlarm by remember { mutableStateOf<AlarmOccurrence?>(null) }
    var calendarCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var snoozeInfo by remember { mutableStateOf(settings.snoozeInfo) }
    var lastSyncTime by remember { mutableStateOf(settings.lastSyncTime) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Diálogo de adicionar calendário
    var showAddSheet by remember { mutableStateOf(false) }
    var availableCalendars by remember { mutableStateOf<List<CalendarAccount>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            availableCalendars = CalendarReader.getAvailableCalendars(context)
            showAddSheet = true
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM", Locale.forLanguageTag("pt-PT")) }

    fun refreshData() {
        val now = System.currentTimeMillis()
        val cals = settings.getCalendars()
        configuredCalendars = cals
        snoozeInfo = settings.snoozeInfo

        // Calcular contadores por calendário e próximo alarme global
        val counts = mutableMapOf<Long, Int>()
        for (cal in cals) {
            if (cal.enabled) {
                val occs = CalendarReader.loadUpcomingOccurrences(
                    context = context,
                    calendarId = cal.id,
                    calendarDisplayName = cal.displayName,
                    calendarColor = cal.color,
                    offsetMinutes = cal.offsetMinutes,
                    disabledKeys = cal.disabledKeys,
                    now = now
                )
                counts[cal.id] = occs?.count { it.isEnabled } ?: 0
            } else {
                counts[cal.id] = 0
            }
        }
        calendarCounts = counts

        nextUpcomingAlarm = AlarmScheduler.getNextUpcomingAlarm(context, now)
        val readOk = AlarmScheduler.scheduleNextAlarm(context)
        if (readOk) {
            settings.lastSyncTime = now
            lastSyncTime = now
        }
    }

    LaunchedEffect(refreshTrigger) {
        refreshData()
    }

    fun openAddCalendar() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            availableCalendars = CalendarReader.getAvailableCalendars(context)
            showAddSheet = true
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CalSyncAlarm",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Botão + (Adicionar Calendário)
                    IconButton(onClick = { openAddCalendar() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar Calendário",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Botão ▶ (Testar)
                    IconButton(onClick = {
                        val testIntent = Intent(context, AlarmReceiver::class.java).apply {
                            action = AlarmReceiver.ACTION_TEST_ALARM
                        }
                        context.sendBroadcast(testIntent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Testar Alarme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Botão ↻ (Atualizar Todos)
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar Todos"
                        )
                    }

                    // Botão ⚙ (Definições Globais)
                    IconButton(onClick = onNavigateToGlobalSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Definições Globais"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Semáforo de permissões inteligente
            item {
                PermissionBanner {
                    refreshTrigger++
                }
            }

            // 2. Cartão Destacado: Próximo Alarme Global
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRÓXIMO ALARME",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            val syncText = if (lastSyncTime > 0) {
                                "Atualizado: ${timeFormatter.format(Date(lastSyncTime))}"
                            } else "A aguardar sincronização"

                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (nextUpcomingAlarm != null) {
                            val alarm = nextUpcomingAlarm!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = timeFormatter.format(Date(alarm.alarmTime)),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = dateFormatter.format(Date(alarm.eventStartTime)).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = alarm.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Tag do Calendário associado
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (alarm.calendarColor != 0) Color(alarm.calendarColor)
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = alarm.calendarDisplayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Nenhum alarme agendado nos próximos 30 dias.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3. Cartão de Snooze Ativo (se existir)
            snoozeInfo?.let { snooze ->
                val now = System.currentTimeMillis()
                if (snooze.triggerTime > now) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Snooze,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Em Snooze: ${snooze.title}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Toca novamente às ${timeFormatter.format(Date(snooze.triggerTime))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        settings.snoozeInfo = null
                                        snoozeInfo = null
                                        NotificationHelper.cancelSnoozeNotification(context)
                                        AlarmScheduler.scheduleNextAlarm(context)
                                        refreshTrigger++
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text("Desligar")
                                }
                            }
                        }
                    }
                }
            }

            // 4. Secção: Os Meus Calendários
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Os Meus Calendários",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${configuredCalendars.size} configurado(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Lista de Calendários Configurados
            if (configuredCalendars.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum calendário adicionado.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque no botão + no topo para associar o primeiro calendário.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { openAddCalendar() }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Adicionar Calendário")
                            }
                        }
                    }
                }
            } else {
                items(configuredCalendars, key = { it.id }) { cal ->
                    val activeCount = calendarCounts[cal.id] ?: 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                onNavigateToCalendarEvents(cal.id)
                            },
                        colors = CardDefaults.cardColors(
                            // Cinzento subtil, um degrau acima do fundo, para o cartão se distinguir
                            containerColor = if (cal.enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = if (cal.enabled) BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ) else null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Indicador de cor do calendário
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (cal.color != 0) Color(cal.color)
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Informação do Calendário
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cal.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (cal.enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = cal.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (cal.enabled) "$activeCount alarmes ativos" else "Calendário desativado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cal.enabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            // Botão ⚙ para abrir as definições deste calendário
                            IconButton(onClick = { onNavigateToCalendarSettings(cal.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Definições de ${cal.displayName}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Switch ON/OFF do calendário
                            Switch(
                                checked = cal.enabled,
                                onCheckedChange = { isChecked ->
                                    settings.toggleCalendar(cal.id, isChecked)
                                    refreshTrigger++
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Sheet para Adicionar Calendário
        if (showAddSheet) {
            val alreadyAddedIds = configuredCalendars.map { it.id }.toSet()
            val filteredCalendars = availableCalendars.filterNot { alreadyAddedIds.contains(it.id) }

            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Adicionar Calendário",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Selecione o calendário pretendido. Serão aplicados os padrões globais que poderá personalizar a qualquer momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (filteredCalendars.isEmpty()) {
                        Text(
                            text = if (availableCalendars.isEmpty()) "Nenhum calendário encontrado no dispositivo."
                            else "Todos os calendários do dispositivo já foram adicionados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCalendars) { cal ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newConfig = CalendarConfig(
                                                id = cal.id,
                                                displayName = cal.displayName,
                                                accountName = cal.accountName,
                                                accountType = cal.accountType,
                                                color = cal.color,
                                                enabled = true,
                                                offsetMinutes = settings.defaultOffsetMinutes,
                                                soundUri = settings.defaultAlarmSoundUri,
                                                volumePercent = settings.defaultVolumePercent,
                                                vibrate = settings.defaultVibrate,
                                                snoozeMinutes = settings.defaultSnoozeMinutes,
                                                preAlarmMinutes = settings.defaultPreAlarmMinutes
                                            )
                                            settings.saveCalendar(newConfig)
                                            showAddSheet = false
                                            refreshTrigger++
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (cal.color != 0) Color(cal.color)
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cal.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = cal.accountName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

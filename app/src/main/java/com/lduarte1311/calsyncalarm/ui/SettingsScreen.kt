package com.lduarte1311.calsyncalarm.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.lduarte1311.calsyncalarm.data.AppSettings
import com.lduarte1311.calsyncalarm.data.CalendarConfig
import com.lduarte1311.calsyncalarm.engine.AlarmScheduler
import com.lduarte1311.calsyncalarm.ui.components.WheelPicker
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke

/**
 * Ecrã de Definições limpo sem numeração.
 * Suporta dois modos:
 * 1. Definições Globais (calendarId == null): padrões para novos calendários.
 * 2. Definições de Calendário (calendarId != null): regras isoladas com botão de remoção.
 *
 * Inclui opções pré-definidas e suporte para Tempo Personalizado nos 3 seletores de tempo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    calendarId: Long? = null,
    onNavigateBack: () -> Unit,
    onCalendarRemoved: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    val isGlobalMode = calendarId == null
    val calendarConfig: CalendarConfig? = remember(calendarId) {
        calendarId?.let { settings.getCalendar(it) }
    }

    // Estados locais de configuração
    var offsetMinutes by remember {
        mutableIntStateOf(calendarConfig?.offsetMinutes ?: settings.defaultOffsetMinutes)
    }
    var volumePercent by remember {
        mutableFloatStateOf((calendarConfig?.volumePercent ?: settings.defaultVolumePercent).toFloat())
    }
    var vibrate by remember {
        mutableStateOf(calendarConfig?.vibrate ?: settings.defaultVibrate)
    }
    var snoozeMinutes by remember {
        mutableIntStateOf(calendarConfig?.snoozeMinutes ?: settings.defaultSnoozeMinutes)
    }
    var preAlarmMinutes by remember {
        mutableIntStateOf(calendarConfig?.preAlarmMinutes ?: settings.defaultPreAlarmMinutes)
    }
    var currentSoundUri by remember {
        mutableStateOf(calendarConfig?.soundUri ?: settings.defaultAlarmSoundUri)
    }

    // Diálogos de seleção padrão
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showPreAlarmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Diálogos de tempo personalizado
    var showCustomOffsetDialog by remember { mutableStateOf(false) }
    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
    var showCustomPreAlarmDialog by remember { mutableStateOf(false) }

    // Estados de índice para as rodas de tempo personalizado
    // Offset: horas 0..12, minutos 0..59
    var customOffsetWheelHours by remember { mutableIntStateOf(0) }
    var customOffsetWheelMinutes by remember { mutableIntStateOf(0) }
    // Snooze: minutos 1..120
    var customSnoozeWheelMinutes by remember { mutableIntStateOf(0) } // índice 0 = 1 min
    // Pré-alarme: horas 0..12, minutos 0..59
    var customPreAlarmWheelHours by remember { mutableIntStateOf(0) }
    var customPreAlarmWheelMinutes by remember { mutableIntStateOf(0) }

    fun saveConfig() {
        if (isGlobalMode) {
            settings.defaultOffsetMinutes = offsetMinutes
            settings.defaultVolumePercent = volumePercent.roundToInt()
            settings.defaultVibrate = vibrate
            settings.defaultSnoozeMinutes = snoozeMinutes
            settings.defaultPreAlarmMinutes = preAlarmMinutes
            settings.defaultAlarmSoundUri = currentSoundUri
        } else if (calendarConfig != null) {
            val updated = calendarConfig.copy(
                offsetMinutes = offsetMinutes,
                volumePercent = volumePercent.roundToInt(),
                vibrate = vibrate,
                snoozeMinutes = snoozeMinutes,
                preAlarmMinutes = preAlarmMinutes,
                soundUri = currentSoundUri
            )
            settings.saveCalendar(updated)
            AlarmScheduler.scheduleNextAlarm(context)
        }
    }

    // Formatadores didáticos dos tempos
    fun formatTimeAdvance(mins: Int): String {
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m.toString().padStart(2, '0')}m antes"
            h > 0 -> "${h}h antes"
            else -> "${m}m antes"
        }
    }

    fun formatSnoozeDuration(mins: Int): String {
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
            h > 0 -> "${h} hora(s)"
            else -> "$mins minutos"
        }
    }

    fun formatPreAlarm(mins: Int): String {
        if (mins == 0) return "Desligada"
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m.toString().padStart(2, '0')}m antes"
            h > 0 -> "${h} hora(s) antes"
            else -> "${m}m antes"
        }
    }

    // Obter o nome do toque atual
    fun getSoundTitle(): String {
        return try {
            val uri = currentSoundUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Toque Padrão do Sistema"
        } catch (e: Exception) {
            "Toque Padrão do Sistema"
        }
    }

    var ringtoneTitle by remember { mutableStateOf(getSoundTitle()) }

    // Seletor de som nativo do Android
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri: Uri? = IntentCompat.getParcelableExtra(
                result.data!!,
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
            val uriStr = uri?.toString()
            currentSoundUri = uriStr
            ringtoneTitle = getSoundTitle()
            saveConfig()
        }
    }

    fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecione o Som do Alarme")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            currentSoundUri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGlobalMode) "Definições Globais" else "Definições do Calendário",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!isGlobalMode && calendarConfig != null) {
                            Text(
                                text = "${calendarConfig.displayName} (${calendarConfig.accountName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isGlobalMode) {
                            Text(
                                text = "Padrões aplicados ao adicionar novos calendários",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Opção: Antecedência fixa antes do evento
            item {
                SettingItemCard(
                    icon = Icons.Default.Schedule,
                    title = "Antecedência do Alarme",
                    subtitle = formatTimeAdvance(offsetMinutes),
                    onClick = { showOffsetDialog = true }
                )
            }

            // Opção: Som do alarme
            item {
                SettingItemCard(
                    icon = Icons.Default.GraphicEq,
                    title = "Som do Alarme",
                    subtitle = ringtoneTitle,
                    onClick = { openRingtonePicker() }
                )
            }

            // Opção: Volume do alarme (0-100%)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = "Volume do Alarme (${volumePercent.roundToInt()}%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = volumePercent,
                            onValueChange = {
                                volumePercent = it
                                saveConfig()
                            },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                        Text(
                            text = "O volume será aplicado temporariamente ao tocar e reposto a seguir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Opção: Vibração (ON/OFF)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.size(12.dp))
                            Column {
                                Text(
                                    text = "Vibração",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (vibrate) "Vibração ativa no alarme" else "Sem vibração",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = vibrate,
                            onCheckedChange = {
                                vibrate = it
                                saveConfig()
                            }
                        )
                    }
                }
            }

            // Opção: Duração do Snooze
            item {
                SettingItemCard(
                    icon = Icons.Default.Snooze,
                    title = "Duração do Snooze",
                    subtitle = formatSnoozeDuration(snoozeMinutes),
                    onClick = { showSnoozeDialog = true }
                )
            }

            // Opção: Notificação pré-alarme
            item {
                SettingItemCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notificação Pré-Alarme",
                    subtitle = formatPreAlarm(preAlarmMinutes),
                    onClick = { showPreAlarmDialog = true }
                )
            }

            // Botão discreto para remover este calendário (Apenas no modo de calendário)
            if (!isGlobalMode && calendarConfig != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Remover este Calendário", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Cartão com crédito de autoria discreto
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Calendar Wake v1.3",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Desenvolvido por Luís Duarte",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        if (isGlobalMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://paypal.me/lduarte1311")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                }
                            ) {
                                Text(
                                    text = "☕ Ajudou-te a acordar? Paga-me um café.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Diálogo 1: Antecedência (Presets + Personalizado)
        if (showOffsetDialog) {
            val offsetPresets = listOf(15, 30, 45, 60, 90, 120, 180)
            val isCustomSelected = !offsetPresets.contains(offsetMinutes)

            AlertDialog(
                onDismissRequest = { showOffsetDialog = false },
                title = { Text("Antecedência do Alarme") },
                text = {
                    Column {
                        offsetPresets.forEach { mins ->
                            val label = when {
                                mins % 60 == 0 -> "${mins / 60} hora(s) antes"
                                mins > 60 -> "${mins / 60}h ${mins % 60}m antes"
                                else -> "$mins minutos antes"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        offsetMinutes = mins
                                        saveConfig()
                                        showOffsetDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = offsetMinutes == mins, onClick = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(label)
                            }
                        }

                        // Opção Personalizado...
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    customOffsetWheelHours = (offsetMinutes / 60).coerceIn(0, 12)
                                    customOffsetWheelMinutes = (offsetMinutes % 60).coerceIn(0, 59)
                                    showOffsetDialog = false
                                    showCustomOffsetDialog = true
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isCustomSelected, onClick = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (isCustomSelected) "Personalizado (${formatTimeAdvance(offsetMinutes)})"
                                else "Personalizado..."
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showOffsetDialog = false }) { Text("Fechar") }
                }
            )
        }

        // Diálogo 1.1: Antecedência Personalizada — Roda de Horas e Minutos
        if (showCustomOffsetDialog) {
            val totalMins = customOffsetWheelHours * 60 + customOffsetWheelMinutes
            val isValid = totalMins >= 1

            val horasList = (0..12).map { it.toString().padStart(2, '0') }
            val minutosList = (0..59).map { it.toString().padStart(2, '0') }

            AlertDialog(
                onDismissRequest = { showCustomOffsetDialog = false },
                title = { Text("Antecedência Personalizada") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Quanto tempo antes do início do evento deve o alarme tocar?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Horas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                WheelPicker(
                                    items = horasList,
                                    selectedIndex = customOffsetWheelHours.coerceIn(0, 12),
                                    onSelectionChanged = { customOffsetWheelHours = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Minutos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                WheelPicker(
                                    items = minutosList,
                                    selectedIndex = customOffsetWheelMinutes.coerceIn(0, 59),
                                    onSelectionChanged = { customOffsetWheelMinutes = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (!isValid) {
                            Text(
                                text = "O tempo total deve ser no mínimo 1 minuto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isValid) {
                                offsetMinutes = totalMins
                                saveConfig()
                                showCustomOffsetDialog = false
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomOffsetDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo 2: Snooze (Presets + Personalizado)
        if (showSnoozeDialog) {
            val snoozePresets = listOf(5, 10, 15, 20, 30)
            val isCustomSelected = !snoozePresets.contains(snoozeMinutes)

            AlertDialog(
                onDismissRequest = { showSnoozeDialog = false },
                title = { Text("Duração do Snooze") },
                text = {
                    Column {
                        snoozePresets.forEach { mins ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        snoozeMinutes = mins
                                        saveConfig()
                                        showSnoozeDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = snoozeMinutes == mins, onClick = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("$mins minutos")
                            }
                        }

                        // Opção Personalizado...
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // índice 0 = 1 min; índice N = N+1 min
                                    customSnoozeWheelMinutes = (snoozeMinutes - 1).coerceIn(0, 119)
                                    showSnoozeDialog = false
                                    showCustomSnoozeDialog = true
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isCustomSelected, onClick = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (isCustomSelected) "Personalizado (${formatSnoozeDuration(snoozeMinutes)})"
                                else "Personalizado..."
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSnoozeDialog = false }) { Text("Fechar") }
                }
            )
        }

        // Diálogo 2.1: Snooze Personalizado — Roda de Minutos (1..120)
        if (showCustomSnoozeDialog) {
            // lista de 1 a 120 minutos
            val snoozeList = (1..120).map { it.toString() }
            val selectedSnoozeValue = customSnoozeWheelMinutes + 1 // índice 0 = 1 min

            AlertDialog(
                onDismissRequest = { showCustomSnoozeDialog = false },
                title = { Text("Duração de Snooze Personalizada") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Selecione os minutos de adiamento do alarme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Minutos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        WheelPicker(
                            items = snoozeList,
                            selectedIndex = customSnoozeWheelMinutes.coerceIn(0, 119),
                            onSelectionChanged = { customSnoozeWheelMinutes = it },
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snoozeMinutes = selectedSnoozeValue
                            saveConfig()
                            showCustomSnoozeDialog = false
                        }
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomSnoozeDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo 3: Notificação Pré-alarme (Presets + Personalizado)
        if (showPreAlarmDialog) {
            val preAlarmPresets = listOf(
                0 to "Desligada",
                30 to "30 minutos antes",
                60 to "1 hora antes",
                120 to "2 horas antes"
            )
            val isCustomSelected = preAlarmMinutes > 0 && preAlarmPresets.none { it.first == preAlarmMinutes }

            AlertDialog(
                onDismissRequest = { showPreAlarmDialog = false },
                title = { Text("Notificação Pré-Alarme") },
                text = {
                    Column {
                        preAlarmPresets.forEach { (mins, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        preAlarmMinutes = mins
                                        saveConfig()
                                        showPreAlarmDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = preAlarmMinutes == mins, onClick = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(label)
                            }
                        }

                        // Opção Personalizado...
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    customPreAlarmWheelHours = (preAlarmMinutes / 60).coerceIn(0, 12)
                                    customPreAlarmWheelMinutes = (preAlarmMinutes % 60).coerceIn(0, 59)
                                    showPreAlarmDialog = false
                                    showCustomPreAlarmDialog = true
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isCustomSelected, onClick = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (isCustomSelected) "Personalizado (${formatPreAlarm(preAlarmMinutes)})"
                                else "Personalizado..."
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPreAlarmDialog = false }) { Text("Fechar") }
                }
            )
        }

        // Diálogo 3.1: Pré-Alarme Personalizado — Roda de Horas e Minutos
        if (showCustomPreAlarmDialog) {
            val totalMins = customPreAlarmWheelHours * 60 + customPreAlarmWheelMinutes
            val isValid = totalMins >= 1

            val horasList = (0..12).map { it.toString().padStart(2, '0') }
            val minutosList = (0..59).map { it.toString().padStart(2, '0') }

            AlertDialog(
                onDismissRequest = { showCustomPreAlarmDialog = false },
                title = { Text("Aviso Pré-Alarme Personalizado") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Quanto tempo antes do alarme pretende receber o aviso prévio?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Horas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                WheelPicker(
                                    items = horasList,
                                    selectedIndex = customPreAlarmWheelHours.coerceIn(0, 12),
                                    onSelectionChanged = { customPreAlarmWheelHours = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Minutos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                WheelPicker(
                                    items = minutosList,
                                    selectedIndex = customPreAlarmWheelMinutes.coerceIn(0, 59),
                                    onSelectionChanged = { customPreAlarmWheelMinutes = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (!isValid) {
                            Text(
                                text = "O tempo total deve ser no mínimo 1 minuto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isValid) {
                                preAlarmMinutes = totalMins
                                saveConfig()
                                showCustomPreAlarmDialog = false
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomPreAlarmDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo de Confirmação para Remover Calendário
        if (showDeleteConfirmDialog && calendarConfig != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Remover Calendário") },
                text = {
                    Text("Tem a certeza de que pretende remover o calendário \"${calendarConfig.displayName}\"? Os respetivos alarmes agendados serão desativados.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            settings.removeCalendar(calendarConfig.id)
                            AlarmScheduler.scheduleNextAlarm(context)
                            showDeleteConfirmDialog = false
                            onCalendarRemoved()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remover")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

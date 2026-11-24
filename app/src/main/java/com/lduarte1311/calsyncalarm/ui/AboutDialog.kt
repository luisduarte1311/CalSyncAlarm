package com.lduarte1311.calsyncalarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Diálogo "Sobre e informações": privacidade, aviso legal e licenças de terceiros.
 * Só texto; não usa rede nem permissões.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sobre e informações") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AboutSection(
                    "Privacidade",
                    "A CalSyncAlarm não tem acesso à rede, não envia dados para lado nenhum " +
                        "e não usa analytics nem publicidade. Lê os teus calendários e guarda " +
                        "as definições apenas neste telemóvel. Não recolhe dados pessoais."
                )
                AboutSection(
                    "Aviso legal",
                    "Aplicação gratuita, fornecida \"tal como está\", sem garantias de qualquer " +
                        "tipo. O funcionamento dos alarmes depende do Android e do estado do " +
                        "telemóvel (bateria, permissões, poupança de energia). Não dependas " +
                        "apenas desta aplicação para compromissos críticos."
                )
                AboutSection(
                    "Licenças de terceiros",
                    "Esta aplicação usa componentes Apache License 2.0 (Android Open Source " +
                        "Project, Google): AndroidX Core, Lifecycle, Activity, Jetpack Compose " +
                        "(UI, Material 3 e Material Icons), WorkManager; e a biblioteca padrão " +
                        "do Kotlin (JetBrains). Texto da licença: apache.org/licenses/LICENSE-2.0"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun AboutSection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = body, style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(16.dp))
}

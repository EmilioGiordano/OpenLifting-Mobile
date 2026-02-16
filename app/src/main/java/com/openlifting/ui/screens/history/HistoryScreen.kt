package com.openlifting.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlifting.data.model.BalanceStatus
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onSessionClick: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Historial") })
        }
    ) { padding ->
        if (uiState.sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay sesiones registradas")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sessions) { summary ->
                    SessionCard(summary = summary, onClick = { onSessionClick(summary.session.id) })
                }
            }
        }
    }
}

@Composable
fun SessionCard(summary: SessionSummary, onClick: () -> Unit) {
    val session = summary.session
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    val (statusIcon, statusColor) = when (summary.balanceStatus) {
        BalanceStatus.GOOD -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
        BalanceStatus.WARNING -> Icons.Filled.Warning to MaterialTheme.colorScheme.tertiary
        BalanceStatus.ALERT -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.date.format(formatter),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalReps = session.series.sumOf { it.repetitions.size }
                val maxWeight = session.series.maxOfOrNull { it.weightKg }?.toInt() ?: 0
                Text(
                    text = "${session.series.size} series | $totalReps reps | Max: ${maxWeight}kg",
                    style = MaterialTheme.typography.bodyMedium
                )
                summary.alertSummary?.let { alert ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                statusIcon,
                contentDescription = summary.balanceStatus.displayName,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

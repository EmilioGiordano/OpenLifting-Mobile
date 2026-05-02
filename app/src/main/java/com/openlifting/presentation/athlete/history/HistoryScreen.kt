package com.openlifting.presentation.athlete.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.presentation.common.toColor
import com.openlifting.presentation.common.toSoftColor
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (sessions.isEmpty()) {
            EmptyHistoryState()
        } else {
            HistoryList(sessions = sessions, onSessionClick = onSessionClick)
        }
    }
}

@Composable
private fun HistoryList(
    sessions: List<SessionHistoryItem>,
    onSessionClick: (Long) -> Unit
) {
    val grouped = sessions.groupByMonth()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text(
                text  = "Historial",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${sessions.size} sesión${if (sessions.size == 1) "" else "es"} registrada${if (sessions.size == 1) "" else "s"}",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            Spacer(Modifier.height(12.dp))
        }

        grouped.forEach { (label, monthItems) ->
            item {
                Text(
                    text  = label.uppercase(),
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(monthItems) { entry ->
                SessionRow(item = entry, onClick = { onSessionClick(entry.sessionId) })
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SessionRow(item: SessionHistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = formatDate(item.startedAt),
                style = MonoText.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = buildSubline(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.olExtras.ink3
            )
        }

        // BSA chip
        if (item.bsaWorstPct > 0f) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(item.overallRisk.toSoftColor())
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "BSA ${"%.0f".format(item.bsaWorstPct)}%",
                    style = MonoText.labelSmall,
                    color = item.overallRisk.toColor()
                )
            }
        }

        RiskBadge(item.overallRisk)

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.olExtras.ink3,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text  = "Historial",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.olExtras.emeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "—",
                    style = MonoText.displaySmall,
                    color = MaterialTheme.olExtras.emerald
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Sin sesiones registradas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "Tus sesiones aparecerán acá una vez que registres la primera.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private val DAY_FMT   = SimpleDateFormat("dd MMM, HH:mm", Locale("es"))
private val MONTH_FMT = SimpleDateFormat("MMMM yyyy", Locale("es"))

private fun formatDate(timestamp: Long): String =
    DAY_FMT.format(Date(timestamp))
        .replaceFirstChar { it.uppercase() }

private fun buildSubline(item: SessionHistoryItem): String = buildString {
    append("${item.setCount} serie${if (item.setCount == 1) "" else "s"}")
    if (item.maxLoadKg > 0f) append(" · ${item.maxLoadKg.toInt()} kg máx")
}

private fun List<SessionHistoryItem>.groupByMonth(): List<Pair<String, List<SessionHistoryItem>>> {
    val cal = Calendar.getInstance()
    return this
        .sortedByDescending { it.startedAt }
        .groupBy {
            cal.timeInMillis = it.startedAt
            MONTH_FMT.format(cal.time).replaceFirstChar { c -> c.uppercase() }
        }
        .toList()
}

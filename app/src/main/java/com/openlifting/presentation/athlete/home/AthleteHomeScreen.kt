package com.openlifting.presentation.athlete.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.presentation.common.BrandMark
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@Composable
fun AthleteHomeScreen(
    onNewSession: () -> Unit,
    onCalibrate: () -> Unit = {},
    viewModel: AthleteHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = uiState) {
            is AthleteHomeUiState.Loading -> LoadingView()
            is AthleteHomeUiState.Empty   -> EmptyView(state, onNewSession, onCalibrate)
            is AthleteHomeUiState.Loaded  -> LoadedView(state, onNewSession, onCalibrate)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color      = MaterialTheme.olExtras.emerald,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun EmptyView(state: AthleteHomeUiState.Empty, onNewSession: () -> Unit, onCalibrate: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { HomeTopBar(firstName = state.athleteFirstName, lastSessionLine = "Sin sesiones aún") }
        if (!state.mvcCalibrated) {
            item { PendingCalibrationBanner(onCalibrate = onCalibrate) }
        }
        item { HomeEmptyState(onStartFirstSession = onNewSession) }
        item {
            // Without MVC calibration, %MVC values during a session have no baseline —
            // we block "Nueva sesión" until the athlete calibrates.
            if (state.mvcCalibrated) {
                PrimaryActionButton(
                    onClick = onNewSession,
                    label   = "Comenzar primera sesión",
                    icon    = Icons.Filled.Add
                )
            } else {
                PrimaryActionButton(
                    onClick = onCalibrate,
                    label   = "Calibrar MVC para empezar",
                    icon    = Icons.Filled.Tune
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun LoadedView(state: AthleteHomeUiState.Loaded, onNewSession: () -> Unit, onCalibrate: () -> Unit) {
    val bsaDelta = state.bsaTrend.takeIf { it.size >= 2 }?.let {
        it.last().value - it[it.size - 2].value
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            HomeTopBar(
                firstName       = state.athleteFirstName,
                lastSessionLine = relativeDate(state.lastSession.startedAt)
            )
        }

        if (!state.mvcCalibrated) {
            item { PendingCalibrationBanner(onCalibrate = onCalibrate) }
        }

        item {
            LastSessionCard(
                summary = state.lastSession,
                onClick = { /* TODO: open SessionDetail */ }
            )
        }

        item {
            BsaTrendCard(
                points          = state.bsaTrend,
                currentValue    = state.lastSession.bsaWorstPct,
                deltaVsPrevious = bsaDelta
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricDeltaChip(
                    label    = "ES:GMax",
                    value    = "%.2f".format(state.esGmax.current),
                    delta    = state.esGmax.deltaVsPrevious,
                    risk     = state.esGmax.risk,
                    isHigherWorse = true,
                    modifier = Modifier.weight(1f)
                )
                MetricDeltaChip(
                    label    = "H:Q",
                    value    = "%.2f".format(state.hq.current),
                    delta    = state.hq.deltaVsPrevious,
                    risk     = state.hq.risk,
                    isHigherWorse = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            if (state.mvcCalibrated) {
                PrimaryActionButton(
                    onClick = onNewSession,
                    label   = "Nueva sesión",
                    icon    = Icons.Filled.Add
                )
            } else {
                PrimaryActionButton(
                    onClick = onCalibrate,
                    label   = "Calibrar MVC para empezar",
                    icon    = Icons.Filled.Tune
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HomeTopBar(firstName: String, lastSessionLine: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark()
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = "Hola, $firstName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End
            )
            Text(
                text  = lastSessionLine,
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Button(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .height(20.dp)
                .padding(end = 8.dp)
        )
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun relativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val days = diffMs / 86_400_000L
    return when {
        days < 1L  -> "Última sesión hoy"
        days == 1L -> "Última sesión ayer"
        days < 7L  -> "Última sesión hace $days días"
        days < 30L -> "Última sesión hace ${days / 7L} semana${if (days / 7L == 1L) "" else "s"}"
        else       -> "Última sesión hace más de un mes"
    }.replaceFirstChar { it.uppercase() }
}

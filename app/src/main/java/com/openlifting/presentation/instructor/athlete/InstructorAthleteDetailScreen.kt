package com.openlifting.presentation.instructor.athlete

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.presentation.common.toColor
import com.openlifting.presentation.common.toSoftColor
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_FMT = SimpleDateFormat("dd MMM, HH:mm", Locale("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorAthleteDetailScreen(
    profileId: Long,
    onBack: () -> Unit,
    onStartSession: (athleteUserId: Long, instructorUserId: Long) -> Unit,
    onRecalibrate: (profileId: Long) -> Unit,
    onSessionClick: (sessionId: Long) -> Unit,
    viewModel: InstructorAthleteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(profileId) { viewModel.load(profileId) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    val title = (state as? InstructorAthleteDetailUiState.Loaded)
                        ?.data?.athlete?.profile?.fullName
                        ?: "Detalle del atleta"
                    Text(title, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            when (val s = state) {
                InstructorAthleteDetailUiState.Loading -> Loading()
                InstructorAthleteDetailUiState.NotFound -> NotFound()
                is InstructorAthleteDetailUiState.Loaded -> Loaded(
                    data           = s.data,
                    onStartSession = { onStartSession(s.data.athlete.athleteUserId, s.data.instructorUserId) },
                    onRecalibrate  = { onRecalibrate(s.data.athlete.profile.id) },
                    onSessionClick = onSessionClick
                )
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color       = MaterialTheme.olExtras.emerald,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun NotFound() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = "Atleta no encontrado",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun Loaded(
    data: InstructorAthleteDetailUiData,
    onStartSession: () -> Unit,
    onRecalibrate: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item { ProfileCard(data) }

        item { SectionLabel("HISTORIAL", topPadding = 6.dp) }
        if (data.recentSessions.isEmpty()) {
            item { EmptyHistory() }
        } else {
            items(data.recentSessions) { row ->
                SessionRow(row = row, onClick = { onSessionClick(row.sessionId) })
            }
        }

        if (data.mvc.isNotEmpty()) {
            item { SectionLabel("CALIBRACIÓN MVC", topPadding = 8.dp) }
            item { MvcTable(data.mvc) }
        }

        item {
            Spacer(Modifier.height(8.dp))
            ActionsBlock(
                isCalibrated   = data.athlete.profile.calibratedAt != null,
                onStartSession = onStartSession,
                onRecalibrate  = onRecalibrate
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActionsBlock(
    isCalibrated: Boolean,
    onStartSession: () -> Unit,
    onRecalibrate: () -> Unit
) {
    if (isCalibrated) {
        // Happy path: athlete is calibrated, full session controls available.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick  = onStartSession,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                )
                Text("Iniciar sesión con este atleta", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick  = onRecalibrate,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Recalibrar MVC")
            }
        }
    } else {
        // Calibration required first: %MVC during the session has no baseline without it,
        // so the metrics would be meaningless. Calibrate is the only path forward.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Inline warning banner explaining why "Iniciar sesión" is disabled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.olExtras.warnSoft)
                    .border(1.dp, MaterialTheme.olExtras.warn.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.olExtras.warn,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text  = "Sin MVC calibrado, los porcentajes de activación no tienen referencia. Calibrá primero para registrar mediciones precisas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.olExtras.warn
                )
            }

            // Primary CTA is now Calibrate
            Button(
                onClick  = onRecalibrate,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                )
                Text("Calibrar MVC primero", style = MaterialTheme.typography.labelLarge)
            }

            // Disabled "Iniciar sesión" so the affordance is visible but locked
            OutlinedButton(
                onClick  = onStartSession,
                enabled  = false,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Iniciar sesión con este atleta")
            }
        }
    }
}

@Composable
private fun ProfileCard(data: InstructorAthleteDetailUiData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.olExtras.emeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = data.athlete.profile.fullName.initials(),
                    style = MonoText.titleLarge,
                    color = MaterialTheme.olExtras.emerald
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = data.athlete.profile.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = data.athlete.profile.sex.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.olExtras.ink3
                )
            }
            TypeTag(isGuest = data.athlete.isGuest)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Stat("PESO",   "${data.athlete.profile.bodyweightKg.toInt()} kg")
            Stat("EDAD",   "${data.athlete.profile.ageYears} años")
            Stat(
                label = "CALIBR.",
                value = data.athlete.profile.calibratedAt?.let { relative(it) } ?: "—",
                valueColor = if (data.athlete.profile.calibratedAt == null) MaterialTheme.olExtras.warn else null
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MonoText.labelSmall, color = MaterialTheme.olExtras.ink3)
        Text(
            text  = value,
            style = MonoText.titleMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionLabel(label: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text  = label,
        style = MonoText.labelSmall,
        color = MaterialTheme.olExtras.ink3,
        modifier = Modifier.padding(top = topPadding, bottom = 2.dp)
    )
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = "Sin sesiones registradas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionRow(row: AthleteSessionRow, onClick: () -> Unit) {
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
                text  = DATE_FMT.format(Date(row.startedAt)).replaceFirstChar { it.uppercase() },
                style = MonoText.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "${row.setCount} serie${if (row.setCount == 1) "" else "s"} · ${row.maxLoadKg.toInt()} kg máx",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.olExtras.ink3
            )
        }
        RiskBadge(row.overallRisk)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.olExtras.ink3,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MvcTable(values: Map<Pair<Muscle, MuscleSide>, Float>) {
    val muscles = listOf(
        Muscle.VASTUS_LATERALIS, Muscle.VASTUS_MEDIALIS,
        Muscle.GLUTEUS_MAXIMUS,  Muscle.ERECTOR_SPINAE,  Muscle.BICEPS_FEMORIS
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(0.9f))
            Text("IZQ", style = MonoText.labelSmall, color = MaterialTheme.olExtras.ink3, modifier = Modifier.weight(1f))
            Text("DER", style = MonoText.labelSmall, color = MaterialTheme.olExtras.ink3, modifier = Modifier.weight(1f))
        }
        muscles.forEach { muscle ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = muscle.shortName,
                    style = MonoText.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.9f)
                )
                MvcCell(values[muscle to MuscleSide.LEFT],  modifier = Modifier.weight(1f))
                MvcCell(values[muscle to MuscleSide.RIGHT], modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MvcCell(value: Float?, modifier: Modifier = Modifier) {
    Text(
        text  = value?.let { "${it.toInt()}%" } ?: "—",
        style = MonoText.bodyMedium,
        color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.olExtras.ink3,
        modifier = modifier
    )
}

@Composable
private fun TypeTag(isGuest: Boolean) {
    val (label, fg, bg) = if (isGuest) {
        Triple("INVITADO", MaterialTheme.olExtras.warn, MaterialTheme.olExtras.warnSoft)
    } else {
        Triple("REGISTRADO", MaterialTheme.olExtras.emerald, MaterialTheme.olExtras.emeraldSoft)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = MonoText.labelSmall, color = fg)
    }
}

private fun relative(timestamp: Long): String {
    val days = (System.currentTimeMillis() - timestamp) / 86_400_000L
    return when {
        days < 1L  -> "hoy"
        days == 1L -> "ayer"
        days < 7L  -> "hace $days días"
        days < 30L -> "hace ${days / 7L} sem"
        else       -> "hace +1 mes"
    }
}

private fun String.initials(): String =
    this.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifBlank { "?" }

package com.openlifting.presentation.instructor.home

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.repository.ManagedAthlete
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@Composable
fun InstructorHomeScreen(
    onCreateGuest: () -> Unit,
    onAthleteClick: (athleteProfileId: Long) -> Unit,
    viewModel: InstructorHomeViewModel = hiltViewModel()
) {
    val data by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = data) {
                null              -> LoadingBlock()
                else              -> if (state.isEmpty) EmptyBlock(state) else ListBlock(state, onAthleteClick)
            }

            // Sticky FAB
            ExtendedFloatingActionButton(
                onClick   = onCreateGuest,
                text      = { Text("Nuevo invitado", style = MaterialTheme.typography.labelLarge) },
                icon      = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color       = MaterialTheme.olExtras.emerald,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun EmptyBlock(state: InstructorHomeUiData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(16.dp))
        Header(state.instructorFirstName, athletesCount = 0)
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.olExtras.emeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.olExtras.emerald,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Aún no tenés atletas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "Creá un invitado para registrar mediciones, o generá un código QR para que un atleta registrado se vincule con vos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListBlock(state: InstructorHomeUiData, onAthleteClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Header(state.instructorFirstName, athletesCount = state.athletes.size)
            Spacer(Modifier.height(8.dp))
        }

        if (state.guestCount > 0) {
            item {
                SectionLabel(
                    label = "INVITADOS · ${state.guestCount}",
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
            }
            items(state.athletes.filter { it.isGuest }) { athlete ->
                AthleteRow(athlete = athlete, onClick = { onAthleteClick(athlete.profile.id) })
            }
        }

        if (state.registeredCount > 0) {
            item {
                SectionLabel(
                    label = "REGISTRADOS · ${state.registeredCount}",
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                )
            }
            items(state.athletes.filter { !it.isGuest }) { athlete ->
                AthleteRow(athlete = athlete, onClick = { onAthleteClick(athlete.profile.id) })
            }
        }

        item { Spacer(Modifier.height(96.dp)) } // padding for FAB
    }
}

@Composable
private fun Header(firstName: String, athletesCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = "Mis atletas",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text  = if (athletesCount == 0) "Hola, $firstName" else "Hola, $firstName · $athletesCount atleta${if (athletesCount == 1) "" else "s"}",
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3
        )
    }
}

@Composable
private fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text  = label,
        style = MonoText.labelSmall,
        color = MaterialTheme.olExtras.ink3,
        modifier = modifier
    )
}

@Composable
private fun AthleteRow(athlete: ManagedAthlete, onClick: () -> Unit) {
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
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.olExtras.emeraldSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = athlete.profile.fullName.initials(),
                style = MonoText.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.olExtras.emerald
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = athlete.profile.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = buildSubline(athlete),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.olExtras.ink3
            )
        }

        // Type tag
        TypeTag(isGuest = athlete.isGuest)

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.olExtras.ink3,
            modifier = Modifier.size(16.dp)
        )
    }
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
        Text(
            text  = label,
            style = MonoText.labelSmall,
            color = fg
        )
    }
}

private fun buildSubline(athlete: ManagedAthlete): String = buildString {
    append("${athlete.profile.bodyweightKg.toInt()} kg · ${athlete.profile.ageYears} años")
    if (athlete.profile.calibratedAt == null) append(" · sin calibrar")
}

private fun String.initials(): String =
    this.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifBlank { "?" }

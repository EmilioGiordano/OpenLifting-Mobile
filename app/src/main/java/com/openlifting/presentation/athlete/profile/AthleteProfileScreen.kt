package com.openlifting.presentation.athlete.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.ThemeMode
import com.openlifting.presentation.common.profile.ProfileViewModel
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@Composable
fun AthleteProfileScreen(
    onLogout: () -> Unit,
    onSwitchToInstructor: () -> Unit,
    onRecalibrate: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onSetupProfile: () -> Unit = {},
    onClaimSession: () -> Unit = {},
    profileViewModel: ProfileViewModel = hiltViewModel(),
    athleteViewModel: AthleteProfileViewModel = hiltViewModel()
) {
    val user      by profileViewModel.user.collectAsState()
    val themeMode by profileViewModel.themeMode.collectAsState()
    val athleteState by athleteViewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text  = "Perfil",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            ProfileIdentityCard(name = user?.name ?: "—", email = user?.email ?: "—")

            ProfileSection(label = "DATOS FÍSICOS") {
                AthleteDataCard(state = athleteState)
                when (athleteState) {
                    is AthleteProfileUiState.Loaded -> {
                        OutlinedButton(
                            onClick  = onEditProfile,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Editar datos")
                        }
                    }
                    AthleteProfileUiState.Missing -> {
                        OutlinedButton(
                            onClick  = onSetupProfile,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Configurar perfil")
                        }
                    }
                    AthleteProfileUiState.Loading -> Unit
                }
            }

            ProfileSection(label = "PREFERENCIAS") {
                ThemeToggleRow(
                    current = themeMode,
                    onChange = profileViewModel::setThemeMode
                )
            }

            ProfileSection(label = "CALIBRACIÓN") {
                OutlinedButton(
                    onClick  = onRecalibrate,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Recalibrar MVC")
                }
            }

            ProfileSection(label = "RECLAMAR SESIÓN") {
                Text(
                    text  = "Si tu entrenador te midió con su cuenta, ingresá el código que te dio para vincular esa sesión a tu perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick  = onClaimSession,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Reclamar sesión con código")
                }
            }

            ProfileSection(label = "CUENTA") {
                OutlinedButton(
                    onClick  = onSwitchToInstructor,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Modo demo: cambiar a Entrenador")
                }
                TextButton(
                    onClick  = { profileViewModel.logout(onLogout) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cerrar sesión",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AthleteDataCard(state: AthleteProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state) {
            AthleteProfileUiState.Loading -> {
                Text(
                    text  = "Cargando datos…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AthleteProfileUiState.Missing -> {
                Text(
                    text  = "Aún no completaste tus datos físicos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "Sin estos datos, los análisis usan valores de referencia genéricos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is AthleteProfileUiState.Loaded -> {
                val profile = state.profile
                DataRow("Peso", "${profile.bodyweightKg.toInt()} kg", mono = true)
                DataRow("Edad", "${profile.ageYears} años", mono = true)
                DataRow("Sexo", profile.sex.displayName)
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = value,
            style = if (mono) MonoText.bodyMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileIdentityCard(name: String, email: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.olExtras.emeraldSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = name.initials(),
                style = MonoText.titleMedium,
                color = MaterialTheme.olExtras.emerald
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text  = label,
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeToggleRow(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = "Tema",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val opts = ThemeMode.entries
            opts.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = current == mode,
                    onClick  = { onChange(mode) },
                    shape    = SegmentedButtonDefaults.itemShape(index = idx, count = opts.size),
                    label    = { Text(mode.displayName) }
                )
            }
        }
    }
}

private fun String.initials(): String =
    this.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifBlank { "?" }

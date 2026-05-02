package com.openlifting.presentation.instructor.profile

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorProfileScreen(
    onLogout: () -> Unit,
    onSwitchToAthlete: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user      by viewModel.user.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

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

            // Identity card with role chip
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
                        text  = (user?.name ?: "?").initialsForProfile(),
                        style = MonoText.titleMedium,
                        color = MaterialTheme.olExtras.emerald
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text  = user?.name ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = "Entrenador · ${user?.email ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Preferencias
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text  = "PREFERENCIAS",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                Text(
                    text  = "Tema",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val opts = ThemeMode.entries
                    opts.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick  = { viewModel.setThemeMode(mode) },
                            shape    = SegmentedButtonDefaults.itemShape(index = idx, count = opts.size),
                            label    = { Text(mode.displayName) }
                        )
                    }
                }
            }

            // Cuenta
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text  = "CUENTA",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick  = onSwitchToAthlete,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Modo demo: cambiar a Atleta")
                }
                TextButton(
                    onClick  = { viewModel.logout(onLogout) },
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

private fun String.initialsForProfile(): String =
    this.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifBlank { "?" }

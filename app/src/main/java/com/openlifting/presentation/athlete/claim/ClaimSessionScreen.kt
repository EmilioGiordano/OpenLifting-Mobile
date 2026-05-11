package com.openlifting.presentation.athlete.claim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimSessionScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ClaimSessionViewModel = hiltViewModel()
) {
    val code    by viewModel.code.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title          = { Text("Reclamar sesión", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            when (val state = uiState) {
                is ClaimSessionUiState.Success -> SuccessContent(onContinue = onSuccess)
                else -> InputContent(
                    code     = code,
                    state    = state,
                    isReady  = viewModel.isReady,
                    onChange = viewModel::setCode,
                    onSubmit = viewModel::submit
                )
            }
        }
    }
}

@Composable
private fun InputContent(
    code: String,
    state: ClaimSessionUiState,
    isReady: Boolean,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text  = "Ingresá el código de 8 caracteres que te dio tu entrenador. La sesión y la calibración asociada quedarán en tu cuenta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value         = code,
            onValueChange = onChange,
            label         = { Text("Código") },
            singleLine    = true,
            isError       = state is ClaimSessionUiState.Error,
            textStyle     = MonoText.titleLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType   = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text  = "${code.length} / 8",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state is ClaimSessionUiState.Error) {
            Text(
                text  = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = onSubmit,
            enabled  = isReady && state !is ClaimSessionUiState.Submitting,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state is ClaimSessionUiState.Submitting) {
                CircularProgressIndicator(
                    modifier   = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Reclamar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SuccessContent(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.olExtras.emeraldSoft, shape = RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.olExtras.emerald,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text  = "¡Sesión reclamada!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text  = "La sesión que tu entrenador midió ya aparece en tu historial. Si no tenías perfil, también copiamos tus datos físicos y tu calibración.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick  = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Ver historial", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al perfil")
        }
    }
}

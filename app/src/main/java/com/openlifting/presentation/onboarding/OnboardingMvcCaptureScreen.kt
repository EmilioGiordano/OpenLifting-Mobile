package com.openlifting.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.MuscleSide
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@Composable
fun OnboardingMvcCaptureScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.mvc.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onComplete()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // Top row: Cancel + step chip
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancelar", color = MaterialTheme.olExtras.ink3, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text  = state.stepLabel,
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
            }

            Spacer(Modifier.height(8.dp))
            StepProgressBar(currentIndex = state.currentIndex, total = state.totalSteps)

            Spacer(Modifier.height(24.dp))

            // Muscle title block
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = "CALIBRANDO",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = state.current.muscle.displayName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                MuscleSidePill(side = state.current.side)
            }

            Spacer(Modifier.height(24.dp))

            // Diagram placeholder
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                MuscleDiagramPlaceholder(
                    sideLetter = if (state.current.side == MuscleSide.LEFT) "L" else "R",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(20.dp))

            BigActivationBar(
                pct   = state.livePct,
                label = if (state.phase == CapturePhase.DONE) "PICO CAPTURADO" else "ACTIVACIÓN",
                modifier = Modifier
            )

            Spacer(Modifier.height(20.dp))

            // Phase-specific instruction + countdown / footer
            when (state.phase) {
                CapturePhase.PREPARE -> PreparePhase(countdown = state.countdown)
                CapturePhase.CONTRACT -> ContractPhase(seconds = state.countdown)
                CapturePhase.DONE -> DonePhase(
                    capturedPct = state.current.capturedPct ?: 0f,
                    isLast      = state.currentIndex == state.totalSteps - 1,
                    onRepeat    = viewModel::repeatCurrent,
                    onNext      = viewModel::next
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PreparePhase(countdown: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text  = "Prepará la contracción",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        CountdownDigit(value = countdown)
    }
}

@Composable
private fun ContractPhase(seconds: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text  = "Contraé al máximo",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.olExtras.emerald,
            textAlign = TextAlign.Center
        )
        Text(
            text  = "00:0$seconds",
            style = MonoText.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DonePhase(
    capturedPct: Float,
    isLast: Boolean,
    onRepeat: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.olExtras.emerald,
                modifier = Modifier
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Captura lista — ${capturedPct.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.olExtras.emerald,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onRepeat,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Repetir") }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(if (isLast) "Finalizar" else "Siguiente")
            }
        }
    }
}

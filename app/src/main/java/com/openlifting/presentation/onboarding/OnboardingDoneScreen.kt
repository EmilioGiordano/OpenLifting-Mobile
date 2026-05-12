package com.openlifting.presentation.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.ui.theme.olExtras

@Composable
fun OnboardingDoneScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val mvc by viewModel.mvc.collectAsState()
    val submission by viewModel.calibrationSubmission.collectAsState()

    val isSubmitting = submission is SubmissionState.Submitting
    val errorMessage = when (val s = submission) {
        is SubmissionState.Error        -> s.message
        SubmissionState.NetworkError    -> "No se pudo conectar a Vortex. Verifique su conexión."
        is SubmissionState.FieldErrors  -> s.errors.values.firstOrNull()?.firstOrNull()
            ?: "Validación fallida. Reintente."
        else -> null
    }

    val capturedMap: Map<Pair<Muscle, MuscleSide>, Float?> =
        mvc.measurements.associate { (it.muscle to it.side) to it.capturedPct }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.olExtras.emeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.olExtras.emerald,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text  = "Calibración lista",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = "Tus 10 valores MVC quedaron registrados. Desde acá tus análisis usan tu fisiología real.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(28.dp))

            MvcCheckGrid(captured = capturedMap)

            Spacer(Modifier.weight(1f))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick  = { viewModel.finalizeCalibration(onDone = onContinue) },
                enabled  = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier   = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color      = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (errorMessage != null) "Reintentar" else "Ir al inicio",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

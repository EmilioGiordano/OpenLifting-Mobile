package com.openlifting.presentation.onboarding

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@Composable
fun OnboardingMvcExplainScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar (forward-only flow, no back arrow)
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text("Saltar por ahora", color = MaterialTheme.olExtras.ink3, style = MaterialTheme.typography.labelMedium)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text  = "Calibremos tus músculos",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // What is MVC explanation card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.olExtras.emeraldSoft)
                        .border(1.dp, MaterialTheme.olExtras.emerald.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text  = "¿QUÉ ES MVC?",
                        style = MonoText.labelSmall,
                        color = MaterialTheme.olExtras.emerald
                    )
                    Text(
                        text  = "MVC (Maximum Voluntary Contraction) es la activación máxima que podés generar en cada músculo. Lo medimos una vez para usarlo como tu 100% de referencia personal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text  = "Sin esta calibración, los porcentajes de activación que veas durante tus sesiones serán aproximados — basados en valores de literatura, no en tu propia fisiología.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text  = "CÓMO FUNCIONA",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3,
                    modifier = Modifier.padding(top = 4.dp)
                )

                StepLine(num = 1, text = "Te guiamos por 10 mediciones (5 músculos × 2 lados)")
                StepLine(num = 2, text = "En cada una, hacés una contracción máxima de ~3 segundos")
                StepLine(num = 3, text = "Capturamos el peak y lo guardamos como tu MVC personal")

                Text(
                    text  = "PUNTOS A CALIBRAR",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3,
                    modifier = Modifier.padding(top = 8.dp)
                )
                MvcCheckGrid()

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.startCapture(); onStart() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Empezar calibración", style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = "Tarda unos 3 minutos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.olExtras.ink3,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StepLine(num: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.olExtras.emerald)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text  = num.toString(),
                style = MonoText.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

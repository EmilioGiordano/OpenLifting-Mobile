package com.openlifting.presentation.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@Composable
fun OnboardingWelcomeScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // Skip top-right
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopEnd) {
                TextButton(onClick = onSkip) {
                    Text("Saltar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.olExtras.ink3)
                }
            }

            Spacer(Modifier.weight(1f))

            // Hero block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.olExtras.emeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "OL",
                    style = MonoText.displayLarge,
                    color = MaterialTheme.olExtras.emerald
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text  = "Medí lo que entrenás",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "OpenLifting analiza la activación muscular durante tus sentadillas y detecta asimetrías y compensaciones que el ojo no ve.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BulletItem(icon = Icons.Filled.Balance,    label = "Análisis\nbilateral")
                BulletItem(icon = Icons.Filled.Shield,     label = "Detecta\ncompensaciones")
                BulletItem(icon = Icons.Filled.TrendingUp, label = "Seguimiento\nhistórico")
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Comenzar", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text  = "Tarda unos 3 minutos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.olExtras.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun BulletItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.olExtras.emerald,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

package com.openlifting.presentation.common

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

/**
 * Canonical stub screen used while the real implementations are being built.
 *
 * Renders the screen name, an optional state line (for VM-driven screens), and up to two
 * action buttons so navigation flows remain testable end-to-end. Uses the OpenLifting design
 * system (ink/cream CTA, emerald accent for status chips, mono for state labels) — this also
 * doubles as a smoke test for the theme tokens.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    description: String? = null,
    stateLabel: String? = null,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Decorative status dot — proves emerald token is wired
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.olExtras.emerald)
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text  = "PANTALLA EN CONSTRUCCIÓN",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text  = title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (description != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (stateLabel != null) {
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.olExtras.emeraldSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text  = stateLabel,
                        style = MonoText.labelSmall,
                        color = MaterialTheme.olExtras.emerald
                    )
                }
            }

            if (primaryAction != null || secondaryAction != null) {
                Spacer(Modifier.height(32.dp))
                primaryAction?.let { (label, action) ->
                    Button(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
                secondaryAction?.let { (label, action) ->
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
            }
        }
    }
}

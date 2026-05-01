package com.openlifting.presentation.athlete.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AthleteProfileScreen(
    onLogout: () -> Unit,
    onSwitchToInstructor: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Perfil", style = MaterialTheme.typography.headlineMedium)
            Text("Emilio Giordano",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { /* TODO: MVC calibration */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Recalibrar MVC")
            }

            OutlinedButton(onClick = { /* TODO: ESP32 pairing */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Mis dispositivos ESP32")
            }

            OutlinedButton(onClick = { /* TODO: scan QR instructor */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Vincularme a un instructor")
            }

            OutlinedButton(onClick = onSwitchToInstructor, modifier = Modifier.fillMaxWidth()) {
                Text("Cambiar a modo instructor")
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Cerrar sesión") }
        }
    }
}

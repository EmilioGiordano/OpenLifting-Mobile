package com.openlifting.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.UserRole
import com.openlifting.ui.theme.olExtras
import kotlinx.coroutines.delay

/**
 * Auto-routes the user to athlete/instructor home if a session exists, or to Login otherwise.
 *
 * The session check resolves locally (Room lookup) so it returns quickly. We give it a small
 * delay window before falling back to Login — this avoids the brief Login flash that would
 * otherwise happen on cold launch when a session does exist but the Flow hasn't emitted yet.
 */
@Composable
fun SplashScreen(
    onSessionFound: (UserRole) -> Unit,
    onNoSession: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkSession()
        delay(400)
        if (viewModel.uiState.value is LoginUiState.Idle) {
            onNoSession()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onSessionFound((uiState as LoginUiState.Success).role)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text  = "OpenLifting",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Análisis electromiográfico para powerlifting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                modifier   = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color      = MaterialTheme.olExtras.emerald
            )
        }
    }
}

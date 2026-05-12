package com.openlifting.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.usecase.auth.StartRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (StartRoute) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role     by remember { mutableStateOf(UserRole.ATHLETE) }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onRegisterSuccess((uiState as LoginUiState.Success).route)
        }
    }

    val isLoading = uiState is LoginUiState.Loading
    val canSubmit = !isLoading && name.isNotBlank() && email.isNotBlank() && password.length >= 8

    val fieldErrors = (uiState as? LoginUiState.FieldErrors)?.errors ?: emptyMap()
    val generalError = uiState.toGeneralMessage()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Crear cuenta", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Text(
                    "Vamos a crear su cuenta — después configuramos su perfil y MVC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                FieldWithError(
                    value         = name,
                    onValueChange = {
                        name = it
                        viewModel.clearTransientError()
                    },
                    label = "Nombre",
                    error = fieldErrors["name"]?.firstOrNull()
                )

                FieldWithError(
                    value         = email,
                    onValueChange = {
                        email = it
                        viewModel.clearTransientError()
                    },
                    label = "Email",
                    error = fieldErrors["email"]?.firstOrNull(),
                    keyboardType = KeyboardType.Email
                )

                FieldWithError(
                    value         = password,
                    onValueChange = {
                        password = it
                        viewModel.clearTransientError()
                    },
                    label = "Contraseña (8+ caracteres)",
                    error = fieldErrors["password"]?.firstOrNull(),
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Soy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val opts = UserRole.entries
                    opts.forEachIndexed { idx, r ->
                        SegmentedButton(
                            selected = role == r,
                            onClick  = { role = r },
                            shape    = SegmentedButtonDefaults.itemShape(index = idx, count = opts.size),
                            label    = { Text(if (r == UserRole.ATHLETE) "Atleta" else "Entrenador") }
                        )
                    }
                }

                if (generalError != null) {
                    Text(
                        text  = generalError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Button(
                        onClick  = { viewModel.register(name, email, password, role) },
                        enabled  = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier   = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color      = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Crear cuenta", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldWithError(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text(label) },
            singleLine    = true,
            isError       = error != null,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text  = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

private fun LoginUiState.toGeneralMessage(): String? = when (this) {
    is LoginUiState.Error     -> message
    LoginUiState.Throttled    -> "Demasiados intentos. Espere un momento e intente de nuevo."
    LoginUiState.NetworkError -> "No se pudo conectar a Vortex. Verifique su conexión."
    else -> null
}

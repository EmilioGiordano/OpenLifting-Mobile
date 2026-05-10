package com.openlifting.presentation.athlete.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.Sex
import com.openlifting.presentation.onboarding.SubmissionState
import com.openlifting.ui.theme.MonoText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAthleteProfileScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditAthleteProfileViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsState()
    val submission by viewModel.submission.collectAsState()

    val isSubmitting = submission is SubmissionState.Submitting
    val fieldErrors = (submission as? SubmissionState.FieldErrors)?.errors ?: emptyMap()
    val generalMessage = when (val s = submission) {
        is SubmissionState.Error    -> s.message
        SubmissionState.NetworkError -> "No se pudo conectar a Vortex. Verificá tu conexión."
        else -> null
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Editar datos", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                FieldWithError(
                    value = draft.firstName,
                    onValueChange = {
                        viewModel.setFirstName(it)
                        viewModel.clearError()
                    },
                    label = "Nombre",
                    error = draft.firstNameError ?: fieldErrors["first_name"]?.firstOrNull()
                )
                FieldWithError(
                    value = draft.lastName,
                    onValueChange = {
                        viewModel.setLastName(it)
                        viewModel.clearError()
                    },
                    label = "Apellido",
                    error = draft.lastNameError ?: fieldErrors["last_name"]?.firstOrNull()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val bwError = draft.bodyweightError ?: fieldErrors["bodyweight_kg"]?.firstOrNull()
                        OutlinedTextField(
                            value = draft.bodyweightKg,
                            onValueChange = {
                                viewModel.setBodyweight(it)
                                viewModel.clearError()
                            },
                            label = { Text("Peso") },
                            isError = bwError != null,
                            suffix = { Text("kg", style = MonoText.labelMedium) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        bwError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error,
                                 style = MaterialTheme.typography.bodySmall,
                                 modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val ageErr = draft.ageError ?: fieldErrors["age_years"]?.firstOrNull()
                        OutlinedTextField(
                            value = draft.ageYears,
                            onValueChange = {
                                viewModel.setAge(it)
                                viewModel.clearError()
                            },
                            label = { Text("Edad") },
                            isError = ageErr != null,
                            suffix = { Text("años", style = MonoText.labelMedium) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ageErr?.let {
                            Text(it, color = MaterialTheme.colorScheme.error,
                                 style = MaterialTheme.typography.bodySmall,
                                 modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Sexo", style = MaterialTheme.typography.labelLarge,
                     color = MaterialTheme.colorScheme.onSurface)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val opts = listOf(Sex.MALE, Sex.FEMALE)
                    opts.forEachIndexed { idx, s ->
                        SegmentedButton(
                            selected = draft.sex == s,
                            onClick = { viewModel.setSex(s) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = opts.size),
                            label = { Text(s.displayName) }
                        )
                    }
                }
                fieldErrors["sex"]?.firstOrNull()?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 4.dp))
                }

                if (generalMessage != null) {
                    Text(
                        text = generalMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.save(onSaved) },
                    enabled = draft.isValid && !isSubmitting && draft.loaded,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FieldWithError(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

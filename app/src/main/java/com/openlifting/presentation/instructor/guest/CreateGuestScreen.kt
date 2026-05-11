package com.openlifting.presentation.instructor.guest

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.openlifting.ui.theme.MonoText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGuestScreen(
    onCreated: (athleteProfileId: Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: CreateGuestViewModel = hiltViewModel()
) {
    val draft           by viewModel.draft.collectAsState()
    val isSaving        by viewModel.isSaving.collectAsState()
    val fieldErrors     by viewModel.fieldErrors.collectAsState()
    val submissionError by viewModel.submissionError.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Nuevo invitado", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
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
                Text(
                    text  = "Creá una cuenta de invitado para registrar mediciones. Cuando el atleta cree su propia cuenta, podrás transferir todos sus datos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                val firstNameErr = draft.firstNameError ?: fieldErrors["first_name"]
                OutlinedTextField(
                    value         = draft.firstName,
                    onValueChange = viewModel::setFirstName,
                    label         = { Text("Nombre") },
                    singleLine    = true,
                    isError       = firstNameErr != null,
                    supportingText = firstNameErr?.let { { Text(it) } },
                    modifier      = Modifier.fillMaxWidth()
                )
                val lastNameErr = draft.lastNameError ?: fieldErrors["last_name"]
                OutlinedTextField(
                    value         = draft.lastName,
                    onValueChange = viewModel::setLastName,
                    label         = { Text("Apellido") },
                    singleLine    = true,
                    isError       = lastNameErr != null,
                    supportingText = lastNameErr?.let { { Text(it) } },
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bwErr = draft.bodyweightError ?: fieldErrors["bodyweight_kg"]
                    OutlinedTextField(
                        value         = draft.bodyweightKg,
                        onValueChange = viewModel::setBodyweight,
                        label         = { Text("Peso") },
                        suffix        = { Text("kg", style = MonoText.labelMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine    = true,
                        isError       = bwErr != null,
                        supportingText = bwErr?.let { { Text(it) } },
                        modifier      = Modifier.weight(1f)
                    )
                    val ageErr = draft.ageError ?: fieldErrors["age_years"]
                    OutlinedTextField(
                        value         = draft.ageYears,
                        onValueChange = viewModel::setAge,
                        label         = { Text("Edad") },
                        suffix        = { Text("años", style = MonoText.labelMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        isError       = ageErr != null,
                        supportingText = ageErr?.let { { Text(it) } },
                        modifier      = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text("Sexo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val opts = Sex.entries
                    opts.forEachIndexed { idx, s ->
                        SegmentedButton(
                            selected = draft.sex == s,
                            onClick  = { viewModel.setSex(s) },
                            shape    = SegmentedButtonDefaults.itemShape(index = idx, count = opts.size),
                            label    = { Text(s.displayName) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                submissionError?.let { msg ->
                    Text(
                        text  = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.createGuest(onCreated) },
                    enabled = draft.isValid && !isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier   = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color      = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Crear y calibrar", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

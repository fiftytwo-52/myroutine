package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ClassFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDialog(viewModel: ClassFlowViewModel, onFinished: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        dob = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = { /* Force Onboarding */ },
        title = { 
            Text(
                "Welcome to ClassFlow", 
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Please set up your profile to continue. This information is required for report generation.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { },
                    label = { Text("Date of Birth") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    enabled = false,
                    trailingIcon = { Icon(Icons.Default.DateRange, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("School Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && dob.isNotBlank() && schoolName.isNotBlank(),
                onClick = {
                    viewModel.saveTeacher(name = name, email = "", officeLocation = "", subjectSpecialty = "", dob = dob, schoolName = schoolName)
                    viewModel.completeOnboarding()
                    onFinished()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Get Started")
            }
        }
    )
}

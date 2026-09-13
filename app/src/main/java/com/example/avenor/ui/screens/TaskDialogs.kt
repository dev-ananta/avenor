package com.example.avenor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.avenor.model.AvenorClass
import com.example.avenor.model.AvenorTask

@Composable
fun AddTaskDialog(
    classes: List<AvenorClass>,
    onDismiss: () -> Unit,
    onCreate: (AvenorTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var className by remember { mutableStateOf(classes.firstOrNull()?.name.orEmpty()) }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var estimated by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task name") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due date") },
                        placeholder = { Text("MM/DD/YYYY") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = estimated,
                        onValueChange = { value ->
                            estimated = value.filter(Char::isDigit)
                        },
                        label = { Text("Estimated minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onCreate(
                        AvenorTask(
                            id = System.currentTimeMillis(),
                            title = title.trim(),
                            description = description.trim(),
                            className = className.trim(),
                            dueDate = dueDate.trim(),
                            priority = priority.trim().ifBlank { "Medium" },
                            estimatedMinutes = estimated.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                            completed = false
                        )
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

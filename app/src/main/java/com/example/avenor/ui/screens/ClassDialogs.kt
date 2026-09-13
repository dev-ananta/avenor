package com.example.avenor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.avenor.model.AvenorClass

@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onCreate: (AvenorClass) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Class") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Class name") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onCreate(
                        AvenorClass(
                            id = System.currentTimeMillis(),
                            name = name.trim(),
                            teacher = teacher.trim(),
                            room = room.trim()
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

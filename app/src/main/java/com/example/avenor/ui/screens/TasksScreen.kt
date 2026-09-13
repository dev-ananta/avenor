package com.example.avenor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.model.AvenorClass
import com.example.avenor.model.AvenorTask
import com.example.avenor.ui.components.EmptyCard
import com.example.avenor.ui.components.TaskCard
import com.example.avenor.ui.theme.Background
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.PrimaryText

@Composable
fun TasksScreen(
    tasks: List<AvenorTask>,
    classes: List<AvenorClass>,
    onTasksChanged: (List<AvenorTask>) -> Unit
) {
    var showAddTask by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTasks = when (selectedFilter) {
        "Active" -> tasks.filterNot { it.completed }
        "Completed" -> tasks.filter { it.completed }
        else -> tasks
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tasks", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
            IconButton(onClick = { showAddTask = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add task", tint = Accent)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Active", "Completed").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        if (filteredTasks.isEmpty()) {
            EmptyCard("No tasks here.", "Tap + to create your first task.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = {
                            onTasksChanged(
                                tasks.map {
                                    if (it.id == task.id) it.copy(completed = !it.completed) else it
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddTask) {
        AddTaskDialog(
            classes = classes,
            onDismiss = { showAddTask = false },
            onCreate = { newTask ->
                onTasksChanged(tasks + newTask)
                showAddTask = false
            }
        )
    }
}

package com.example.avenor.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.avenor.data.AvenorStorage
import com.example.avenor.model.AvenorClass
import com.example.avenor.model.AvenorEvent
import com.example.avenor.model.AvenorPage
import com.example.avenor.model.AvenorTask
import com.example.avenor.ui.components.AvenorBottomBar
import com.example.avenor.ui.screens.CalendarScreen
import com.example.avenor.ui.screens.ClassesScreen
import com.example.avenor.ui.screens.HomeScreen
import com.example.avenor.ui.screens.ProfileScreen
import com.example.avenor.ui.screens.TasksScreen
import com.example.avenor.ui.theme.Background

@Composable
fun AvenorApp(context: Context = LocalContext.current) {
    val storage = remember(context) { AvenorStorage(context) }

    var currentPage by remember { mutableStateOf(AvenorPage.HOME) }
    var tasks by remember { mutableStateOf(storage.loadTasks()) }
    var classes by remember { mutableStateOf(storage.loadClasses()) }
    var events by remember { mutableStateOf(storage.loadEvents()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                AvenorBottomBar(
                    currentPage = currentPage,
                    onPageSelected = { currentPage = it }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentPage) {
                    AvenorPage.HOME -> HomeScreen(
                        tasks = tasks,
                        events = events,
                        onNavigate = { currentPage = it },
                        onTaskCompleted = { taskId ->
                            tasks = toggleTask(tasks, taskId)
                            storage.saveTasks(tasks)
                        }
                    )
                    AvenorPage.TASKS -> TasksScreen(
                        tasks = tasks,
                        classes = classes,
                        onTasksChanged = {
                            tasks = it
                            storage.saveTasks(it)
                        }
                    )
                    AvenorPage.CALENDAR -> CalendarScreen(
                        events = events,
                        onEventsChanged = {
                            events = it
                            storage.saveEvents(it)
                        }
                    )
                    AvenorPage.CLASSES -> ClassesScreen(
                        classes = classes,
                        onClassesChanged = {
                            classes = it
                            storage.saveClasses(it)
                        }
                    )
                    AvenorPage.PROFILE -> ProfileScreen()
                }
            }
        }
    }
}

private fun toggleTask(tasks: List<AvenorTask>, taskId: Long): List<AvenorTask> =
    tasks.map { task ->
        if (task.id == taskId) task.copy(completed = !task.completed) else task
    }

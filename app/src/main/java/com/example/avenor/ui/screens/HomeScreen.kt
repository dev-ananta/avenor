package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.avenor.model.AvenorEvent
import com.example.avenor.model.AvenorPage
import com.example.avenor.model.AvenorTask
import com.example.avenor.ui.components.EmptyCard
import com.example.avenor.ui.components.QuickAction
import com.example.avenor.ui.components.SectionTitle
import com.example.avenor.ui.components.SimpleTaskRow
import com.example.avenor.ui.components.StatCard
import com.example.avenor.ui.components.TaskCard
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.Background
import com.example.avenor.ui.theme.PrimaryText
import com.example.avenor.ui.theme.SecondaryText

@Composable
fun HomeScreen(
    tasks: List<AvenorTask>,
    events: List<AvenorEvent>,
    onNavigate: (AvenorPage) -> Unit,
    onTaskCompleted: (Long) -> Unit
) {
    val incompleteTasks = tasks.filterNot { it.completed }
    val completedTasks = tasks.count { it.completed }

    LazyColumn(
        modifier = Modifier
            .background(Background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "AVENOR",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = Accent
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Good evening.",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Text(
                text = "Here's what's ahead.",
                fontSize = 16.sp,
                color = SecondaryText
            )
        }

        item {
            SectionTitle("Focus Now")
            Spacer(Modifier.height(8.dp))
            if (incompleteTasks.isEmpty()) {
                EmptyCard("You're all caught up.", "Enjoy the clear schedule.")
            } else {
                TaskCard(
                    task = incompleteTasks.first(),
                    onComplete = { onTaskCompleted(incompleteTasks.first().id) }
                )
            }
        }

        item {
            SectionTitle("Today")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(Modifier.weight(1f), incompleteTasks.size.toString(), "Tasks")
                StatCard(Modifier.weight(1f), events.size.toString(), "Events")
                StatCard(Modifier.weight(1f), completedTasks.toString(), "Done")
            }
        }

        item {
            SectionTitle("Coming Up")
            Spacer(Modifier.height(8.dp))
            if (incompleteTasks.isEmpty()) {
                EmptyCard("Nothing coming up.", "Add a task to get started.")
            } else {
                Column {
                    incompleteTasks.take(4).forEach { task ->
                        SimpleTaskRow(task)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            SectionTitle("Quick Actions")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    title = "Tasks",
                    onClick = { onNavigate(AvenorPage.TASKS) }
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DateRange,
                    title = "Calendar",
                    onClick = { onNavigate(AvenorPage.CALENDAR) }
                )
            }
        }
    }
}

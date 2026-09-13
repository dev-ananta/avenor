package com.example.avenor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.model.AvenorTask
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.Green
import com.example.avenor.ui.theme.Orange
import com.example.avenor.ui.theme.PrimaryText
import com.example.avenor.ui.theme.Red
import com.example.avenor.ui.theme.SecondaryText
import com.example.avenor.ui.theme.Surface as AppSurface

@Composable
fun TaskCard(task: AvenorTask, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onComplete() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textDecoration = if (task.completed) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    }
                )
                if (task.className.isNotBlank()) {
                    Text(task.className, color = SecondaryText, fontSize = 13.sp)
                }
                if (task.dueDate.isNotBlank()) {
                    Text("Due ${task.dueDate}", color = SecondaryText, fontSize = 13.sp)
                }
            }
            PriorityBadge(task.priority)
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val priorityColor = when (priority.trim().lowercase()) {
        "high" -> Red
        "medium" -> Orange
        else -> Green
    }

    Surface(
        color = priorityColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            priority.ifBlank { "Low" },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = priorityColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SimpleTaskRow(task: AvenorTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(task.title, fontWeight = FontWeight.Medium, color = PrimaryText)
                Text(
                    task.dueDate.ifBlank { "No due date" },
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

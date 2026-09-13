package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.avenor.model.AvenorEvent
import com.example.avenor.ui.components.EmptyCard
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.Background
import com.example.avenor.ui.theme.PrimaryText
import com.example.avenor.ui.theme.SecondaryText
import com.example.avenor.ui.theme.Surface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    events: List<AvenorEvent>,
    onEventsChanged: (List<AvenorEvent>) -> Unit
) {
    var showAddEvent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Calendar", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
            IconButton(onClick = { showAddEvent = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add event", tint = Accent)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
            fontSize = 16.sp,
            color = SecondaryText
        )
        Spacer(Modifier.height(20.dp))

        if (events.isEmpty()) {
            EmptyCard("Your calendar is clear.", "Add an event with the + button.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Accent)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(event.title, fontWeight = FontWeight.Bold, color = PrimaryText)
                                Text(event.date, color = SecondaryText, fontSize = 13.sp)
                                Text(
                                    "${event.startTime} - ${event.endTime}",
                                    color = SecondaryText,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEvent) {
        AddEventDialog(
            onDismiss = { showAddEvent = false },
            onCreate = { event ->
                onEventsChanged(events + event)
                showAddEvent = false
            }
        )
    }
}

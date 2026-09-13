package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.avenor.model.AvenorClass
import com.example.avenor.ui.components.EmptyCard
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.Background
import com.example.avenor.ui.theme.PrimaryText
import com.example.avenor.ui.theme.SecondaryText
import com.example.avenor.ui.theme.Surface

@Composable
fun ClassesScreen(
    classes: List<AvenorClass>,
    onClassesChanged: (List<AvenorClass>) -> Unit
) {
    var showAddClass by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Classes", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
            IconButton(onClick = { showAddClass = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add class", tint = Accent)
            }
        }
        Spacer(Modifier.height(18.dp))

        if (classes.isEmpty()) {
            EmptyCard("No classes yet.", "Add your first class.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(classes, key = { it.id }) { schoolClass ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                schoolClass.name,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Spacer(Modifier.height(5.dp))
                            if (schoolClass.teacher.isNotBlank()) {
                                Text(schoolClass.teacher, color = SecondaryText)
                            }
                            if (schoolClass.room.isNotBlank()) {
                                Text(
                                    "Room ${schoolClass.room}",
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

    if (showAddClass) {
        AddClassDialog(
            onDismiss = { showAddClass = false },
            onCreate = { schoolClass ->
                onClassesChanged(classes + schoolClass)
                showAddClass = false
            }
        )
    }
}

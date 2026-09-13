package com.example.avenor.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.avenor.model.AvenorPage
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.Surface

@Composable
fun AvenorBottomBar(
    currentPage: AvenorPage,
    onPageSelected: (AvenorPage) -> Unit
) {
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentPage == AvenorPage.HOME,
            onClick = { onPageSelected(AvenorPage.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentPage == AvenorPage.TASKS,
            onClick = { onPageSelected(AvenorPage.TASKS) },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tasks") },
            label = { Text("Tasks") }
        )
        NavigationBarItem(
            selected = currentPage == AvenorPage.CALENDAR,
            onClick = { onPageSelected(AvenorPage.CALENDAR) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
            label = { Text("Calendar") }
        )
        NavigationBarItem(
            selected = currentPage == AvenorPage.CLASSES,
            onClick = { onPageSelected(AvenorPage.CLASSES) },
            icon = { Icon(Icons.Default.School, contentDescription = "Classes") },
            label = { Text("Classes") }
        )
        NavigationBarItem(
            selected = currentPage == AvenorPage.PROFILE,
            onClick = { onPageSelected(AvenorPage.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.avenor.ui.theme.Background
import com.example.avenor.ui.theme.PrimaryText
import com.example.avenor.ui.theme.SecondaryText
import com.example.avenor.ui.theme.Surface

@Composable
fun ProfileScreen() {
    LazyColumn(
        modifier = Modifier
            .background(Background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Profile", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
        }
        item {
            Spacer(Modifier.padding(top = 1.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Avenor Student",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text("Student", color = SecondaryText)
                }
            }
        }
        item { ProfileOption(Icons.Default.Settings, "Preferences") }
        item { ProfileOption(Icons.Default.Notifications, "Notifications") }
        item { ProfileOption(Icons.Default.Palette, "Appearance") }
        item { ProfileOption(Icons.Default.Info, "About Avenor") }
        item {
            Text("Avenor", color = SecondaryText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProfileOption(icon: ImageVector, title: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SecondaryText)
            Spacer(Modifier.padding(start = 14.dp))
            Text(title, color = PrimaryText, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}

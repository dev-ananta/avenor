package com.example.avenor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.ui.theme.Accent
import com.example.avenor.ui.theme.Green
import com.example.avenor.ui.theme.PrimaryText
import com.example.avenor.ui.theme.SecondaryText
import com.example.avenor.ui.theme.Surface

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryText
    )
}

@Composable
fun StatCard(
    modifier: Modifier,
    number: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(number, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
            Text(label, fontSize = 12.sp, color = SecondaryText)
        }
    }
}

@Composable
fun QuickAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Accent)
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = PrimaryText)
        }
    }
}

@Composable
fun EmptyCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(title, fontWeight = FontWeight.Bold, color = PrimaryText)
            Text(subtitle, color = SecondaryText, fontSize = 13.sp)
        }
    }
}

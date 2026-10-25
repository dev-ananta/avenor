package com.example.avenor.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.GradeLevel
import com.example.avenor.ui.components.ChoiceRow
import com.example.avenor.ui.components.LabeledSwitch
import com.example.avenor.ui.components.Panel
import com.example.avenor.ui.components.SecondaryButton
import com.example.avenor.ui.theme.Av

/** Profile + settings. Opened from the avatar on Today, like most apps students already use (Jakob's Law). */
@Composable
fun YouScreen(store: AvenorStore, onRequestReminders: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val p = store.profile

    Column(
        Modifier.fillMaxSize().background(Av.Background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("You", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Av.HeroGradient), contentAlignment = Alignment.Center) {
                    Text(p.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(p.name.ifBlank { "Student" }, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("${p.grade.label} · ${store.streak}-day streak", color = Av.Muted, fontSize = 14.sp)
                }
            }
        }
        Panel {
            OutlinedTextField(p.name, { v -> store.updateProfile { it.copy(name = v) } }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Text("Grade", color = Av.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            ChoiceRow(GradeLevel.entries.toList(), p.grade, { it.label }) { g -> store.updateProfile { it.copy(grade = g) } }
        }
        Panel {
            Text("Daily rhythm", fontWeight = FontWeight.SemiBold)
            Text("Daily focus goal: ${p.dailyGoalMinutes} min", color = Av.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            ChoiceRow(listOf(30, 45, 60, 90, 120, 180), p.dailyGoalMinutes, { "${it}m" }) { v -> store.updateProfile { it.copy(dailyGoalMinutes = v) } }
            Text("Default session", color = Av.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            ChoiceRow(listOf(15, 20, 25, 30, 45, 50), p.defaultSessionMinutes, { "$it min" }) { v -> store.updateProfile { it.copy(defaultSessionMinutes = v) } }
            Text("Start small. A goal you hit every day beats a big one you miss.", color = Av.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Panel {
            LabeledSwitch("Reminders", p.remindersEnabled) { on ->
                if (on) onRequestReminders() else store.updateProfile { it.copy(remindersEnabled = false) }
            }
            if (p.remindersEnabled) {
                Text("Daily planning nudge", color = Av.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                ChoiceRow((12..21).toList(), p.planReminderHour, { h -> if (h == 12) "12 PM" else "${h - 12} PM" }) { h ->
                    store.updateProfile { it.copy(planReminderHour = h) }
                }
                LabeledSwitch("Nudge me if I leave a session", p.nudgeWhenLeaving) { v -> store.updateProfile { it.copy(nudgeWhenLeaving = v) } }
            }
            Text("Due-date reminders arrive at 7 PM the night before and 7 AM the day of. Everything is scheduled on your phone.",
                color = Av.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Panel {
            Text("Your stats", fontWeight = FontWeight.SemiBold)
            StatLine("Total focus", "${store.totalFocusMinutes / 60}h ${store.totalFocusMinutes % 60}m")
            StatLine("Sessions", "${store.sessions.size}")
            StatLine("Tasks completed", "${store.tasks.count { it.completed }}")
            StatLine("Cards mastered", "${store.cards.count { it.box >= 4 }} of ${store.cards.size}")
        }
        // Research (Wells, Stanford HAI, 2025): AI chatbots can miss signs of crisis — so Avenor points to people.
        Panel {
            Text("Need to talk to someone?", fontWeight = FontWeight.SemiBold)
            SecondaryButton("Call 988 — Suicide & Crisis Lifeline", {
                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:988"))) }
            }, Modifier.padding(top = 10.dp), Icons.Filled.Phone)
            SecondaryButton("Text 988", {
                runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:988"))) }
            }, Modifier.padding(top = 8.dp), Icons.Filled.Sms)
            Text("Free, 24/7, anywhere in the U.S. You can also talk to a parent, teacher or school counselor. Avenor is a study tool, not a substitute for real support.",
                color = Av.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = Av.Muted, modifier = Modifier.size(18.dp))
                Text("  Your data never leaves this device.", fontSize = 14.sp)
            }
            HorizontalDivider(color = Av.Border, modifier = Modifier.padding(vertical = 8.dp))
            Text("Avenor is free — no ads, no subscriptions, no accounts. Made by a Congressional App Challenge team.",
                color = Av.Muted, fontSize = 12.sp)
            TextButton(onClick = { confirmReset = true }, modifier = Modifier.heightIn(min = Av.MinTap)) { Text("Reset all data", color = Av.Red) }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Erase everything?") },
            text = { Text("This erases all tasks, cards and history, and can't be undone.") },
            confirmButton = { TextButton(onClick = { store.resetAll(); confirmReset = false; onClose() }) { Text("Erase", color = Av.Red) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
            containerColor = Av.Elevated,
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = Av.Muted)
    }
}

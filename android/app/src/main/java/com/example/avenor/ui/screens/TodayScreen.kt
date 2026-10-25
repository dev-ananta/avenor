package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.AvenorTask
import com.example.avenor.data.Destination
import com.example.avenor.domain.FocusEngine
import com.example.avenor.domain.Planner
import com.example.avenor.ui.components.Chip
import com.example.avenor.ui.components.Panel
import com.example.avenor.ui.components.PrimaryButton
import com.example.avenor.ui.components.ProgressRing
import com.example.avenor.ui.components.QuickAddBar
import com.example.avenor.ui.components.ScreenTitle
import com.example.avenor.ui.components.SectionHeading
import com.example.avenor.ui.components.StatTile
import com.example.avenor.ui.components.TaskEditor
import com.example.avenor.ui.components.TaskRow
import com.example.avenor.ui.components.classColor
import com.example.avenor.ui.theme.Av
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Hick's Law: one recommended task + one big Start. Von Restorff: the hero card is the only bright block.
 * Miller's Law: max 3 "up next". Peak-End: evening wrap-up.
 */
@Composable
fun TodayScreen(store: AvenorStore, focus: FocusEngine, onOpenProfile: () -> Unit) {
    var skipped by remember { mutableStateOf(setOf<String>()) }
    var editing by remember { mutableStateOf<AvenorTask?>(null) }
    val haptics = LocalHapticFeedback.current
    val ranked = store.rankedActive
    val hero = ranked.firstOrNull { it.id !in skipped } ?: ranked.firstOrNull()
    val upNext = ranked.filter { it.id != hero?.id }.take(3)
    val events = store.schedule(LocalDate.now()).take(3)
    val hour = LocalTime.now().hour
    val session = store.profile.defaultSessionMinutes

    fun complete(task: AvenorTask) {
        store.toggle(task)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) {
                    val base = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
                    val name = store.profile.name.trim()
                    ScreenTitle(if (name.isEmpty()) base else "$base, $name",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
                }
                IconButton(onClick = onOpenProfile, modifier = Modifier.size(Av.MinTap)) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Profile and settings", tint = Av.Accent, modifier = Modifier.size(30.dp))
                }
            }

            // Daily goal ring (goal-gradient)
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(
                        Modifier.size(74.dp).semantics {
                            contentDescription = "${store.todayFocusMinutes} of ${store.profile.dailyGoalMinutes} focus minutes today"
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        ProgressRing(store.dailyGoalProgress, Modifier.fillMaxSize(), 9.dp,
                            if (store.dailyGoalProgress >= 1f) Av.Green else Av.Accent)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${store.todayFocusMinutes}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("of ${store.profile.dailyGoalMinutes}m", color = Av.Muted, fontSize = 11.sp)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goalMessage(store), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(if (store.streak == 1) "1-day streak" else "${store.streak}-day streak", Av.Orange, Icons.Filled.LocalFireDepartment)
                            Chip("${store.tasksDoneToday} done", Av.Green, Icons.Filled.Check)
                        }
                    }
                }
            }

            // FOCUS NOW — Von Restorff hero
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(Av.RadiusXL), ambientColor = Av.Accent, spotColor = Av.Accent)
                    .clip(RoundedCornerShape(Av.RadiusXL))
                    .background(Av.HeroGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("FOCUS NOW", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp)
                if (hero != null) {
                    Text(hero.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 3)
                    Text(Planner.reason(hero, session), color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
                    PrimaryButton("Start $session-min focus", { focus.start(hero.id, hero.title, session) },
                        icon = Icons.Filled.PlayArrow, container = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeroAction("Done", Icons.Filled.Check) { complete(hero) }
                        Box(Modifier.weight(1f))
                        // Survey: 39% delay or distract themselves when stressed. Five minutes is small enough to start.
                        HeroAction("Just 5 min", Icons.Filled.Bolt) { focus.start(hero.id, hero.title, 5) }
                        Box(Modifier.weight(1f))
                        if (ranked.size > 1) HeroAction("Not now", Icons.AutoMirrored.Filled.ArrowForward) {
                            skipped = if (ranked.all { it.id in skipped + hero.id }) emptySet() else skipped + hero.id
                        }
                    }
                } else {
                    Text(if (store.tasks.isEmpty()) "Let's set up your first task" else "All caught up!",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Add something below, or start a free study session right now.", color = Color.White.copy(alpha = 0.85f))
                    PrimaryButton("Start free focus", { focus.start(null, "Free study", session) },
                        icon = Icons.Filled.PlayArrow, container = Color.White)
                }
            }

            if (upNext.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeading("Up next", if (ranked.size > 4) "+${ranked.size - 4} more" else null)
                    Panel {
                        upNext.forEachIndexed { i, task ->
                            TaskRow(store, task, onToggle = { complete(task) }, onOpen = { editing = task })
                            if (i < upNext.lastIndex) HorizontalDivider(color = Av.Border)
                        }
                    }
                    TextButton(onClick = { store.tab = Destination.PLAN }, modifier = Modifier.heightIn(min = Av.MinTap)) {
                        Text("See your whole plan")
                    }
                }
            }

            if (events.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeading("Today's schedule")
                    Panel {
                        events.forEach { e ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(store.classColor(e.classId)))
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(e.title, fontWeight = FontWeight.SemiBold)
                                    Text(eventTimeLabel(e), color = Av.Muted, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            val due = store.dueCards.size
            if (due > 0) {
                Panel(Modifier.clickable { store.tab = Destination.REVIEW }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = Av.Violet,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Av.Violet.copy(alpha = 0.15f)).padding(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (due == 1) "1 card ready to review" else "$due cards ready to review", fontWeight = FontWeight.SemiBold)
                            Text("About ${maxOf(1, due / 4)} min · keeps today's learning from fading", color = Av.Muted, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Av.Muted)
                    }
                }
            }

            if (hour >= 19) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeading("Today's wrap")
                    Panel {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("${store.todayFocusMinutes}m", "focused", Av.Accent)
                            StatTile("${store.tasksDoneToday}", "tasks done", Av.Green)
                            StatTile("${store.reviewsToday}", "cards", Av.Violet)
                        }
                        Text(
                            if (store.todayFocusMinutes + store.tasksDoneToday + store.reviewsToday == 0)
                                "Quiet day — that's okay. Even 10 minutes now keeps your streak alive."
                            else "You showed up today. That's the whole game.",
                            modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.SemiBold,
                        )
                        ranked.firstOrNull()?.let {
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WbTwilight, contentDescription = null, tint = Av.Orange, modifier = Modifier.size(18.dp))
                                Text("  Tomorrow starts with: ${it.title}", color = Av.Orange, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        QuickAddBar(store, Modifier.align(Alignment.BottomCenter).background(Av.Background).padding(horizontal = 16.dp, vertical = 8.dp))
    }
    editing?.let { TaskEditor(store, it) { editing = null } }
}

@Composable
private fun HeroAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = Av.MinTap)) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(" $text", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun goalMessage(store: AvenorStore): String {
    val done = store.todayFocusMinutes
    val goal = store.profile.dailyGoalMinutes
    return when {
        done == 0 -> "One session gets you started."
        done >= goal -> "Goal reached. Anything more is bonus."
        store.dailyGoalProgress >= 0.5f -> "Only ${goal - done} min to today's goal."
        else -> "Nice start — ${goal - done} min to go."
    }
}

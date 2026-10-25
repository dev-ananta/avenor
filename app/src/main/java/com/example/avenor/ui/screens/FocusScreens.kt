package com.example.avenor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.Flashcard
import com.example.avenor.domain.FocusEngine
import com.example.avenor.domain.FocusPhase
import com.example.avenor.domain.Planner
import com.example.avenor.domain.startMillis
import com.example.avenor.ui.components.Chip
import com.example.avenor.ui.components.HoldToEndButton
import com.example.avenor.ui.components.LabeledSwitch
import com.example.avenor.ui.components.Panel
import com.example.avenor.ui.components.PrimaryButton
import com.example.avenor.ui.components.ProgressRing
import com.example.avenor.ui.components.ScreenTitle
import com.example.avenor.ui.components.SecondaryButton
import com.example.avenor.ui.components.SectionHeading
import com.example.avenor.ui.components.StatTile
import com.example.avenor.ui.components.clock
import com.example.avenor.ui.theme.Av
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Study Mode setup: two questions ("what?" and "how long?"), both pre-answered. */
@Composable
fun FocusScreen(store: AvenorStore, focus: FocusEngine) {
    val candidates = store.rankedActive.take(5)                  // Miller's Law
    var selectedId by remember { mutableStateOf(candidates.firstOrNull()?.id) }
    var custom by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(store.profile.defaultSessionMinutes) }
    val presets = (listOf(15, 25, 45) + store.profile.defaultSessionMinutes).distinct().sorted()   // Hick's Law
    val label = custom.trim().ifEmpty { store.task(selectedId)?.title ?: "Free study" }
    val taskId = if (custom.isBlank()) selectedId else null

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ScreenTitle("Focus", "One task. One timer. Phone face-down.")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What are you working on?", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                candidates.forEachIndexed { i, task ->
                    val sel = selectedId == task.id && custom.isBlank()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(Av.RadiusM))
                            .background(if (sel) Av.Accent.copy(alpha = 0.12f) else Av.Surface)
                            .border(1.dp, if (sel) Av.Accent else Av.Border, RoundedCornerShape(Av.RadiusM))
                            .clickable { selectedId = task.id; custom = "" }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(if (sel) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, null,
                            tint = if (sel) Av.Accent else Av.Muted)
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(Planner.reason(task, minutes), color = Av.Muted, fontSize = 12.sp)
                        }
                        if (i == 0) Chip("Suggested", Av.Accent)
                    }
                }
                OutlinedTextField(custom, { custom = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (candidates.isEmpty()) "What are you studying?" else "Or type something else…") })
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How long?", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.forEach { p ->
                        val sel = minutes == p
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 64.dp)
                                .clip(RoundedCornerShape(Av.RadiusM))
                                .background(if (sel) Av.Accent else Av.Surface)
                                .clickable { minutes = p }
                                .semantics { contentDescription = "$p minutes" + if (sel) ", selected" else "" }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("$p", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (sel) Color.Black else Av.Text)
                            Text("min", fontSize = 12.sp, color = if (sel) Color.Black else Av.Muted)
                        }
                    }
                }
                Text(
                    when {
                        minutes <= 15 -> "Great for getting unstuck or a quick review."
                        minutes <= 30 -> "The sweet spot for most homework."
                        else -> "Deep work — best for essays, projects and test prep."
                    }, color = Av.Muted, fontSize = 13.sp,
                )
                TextButton(onClick = { focus.start(taskId, label, 5) }, modifier = Modifier.heightIn(min = Av.MinTap)) {
                    Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(18.dp)); Text("  Stuck? Just start with 5 minutes")
                }
            }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Psychology, null, tint = Av.Accent); Text("  How a round works", fontWeight = FontWeight.SemiBold)
                }
                Text("1. Focus on one thing until the ring fills.\n2. Write one question you can now answer — Avenor turns it into a review card.\n3. Take a real break (no scrolling), then go again.",
                    color = Av.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (store.sessions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeading("Recent sessions", "${store.todayFocusMinutes} min today")
                    Panel {
                        store.sessions.takeLast(4).reversed().forEach { s ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (s.reachedGoal) Icons.Filled.Verified else Icons.Filled.Spa, null,
                                    tint = if (s.reachedGoal) Av.Green else Av.Accent, modifier = Modifier.size(18.dp))
                                Text("  ${s.label}", maxLines = 1, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Text("${s.focusedMinutes} min", color = Av.Muted, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        // Fitts's Law: the action that matters is huge and in the thumb zone.
        PrimaryButton("Start $minutes-min session", { focus.start(taskId, label, minutes) },
            Modifier.align(Alignment.BottomCenter).background(Av.Background).padding(horizontal = 20.dp, vertical = 10.dp),
            icon = Icons.Filled.PlayArrow)
    }
}

/** Full-screen session: running → finished (Peak-End) → break. Nothing else to tap. */
@Composable
fun FocusSessionScreen(store: AvenorStore, focus: FocusEngine) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val haptics = LocalHapticFeedback.current
    BackHandler(enabled = focus.phase == FocusPhase.FOCUSING || focus.phase == FocusPhase.PAUSED) { /* use Hold to end */ }

    LaunchedEffect(focus.phase) {
        while (true) {
            now = System.currentTimeMillis()
            if (focus.phase == FocusPhase.FOCUSING && focus.remaining(now) <= 0) {
                focus.complete()?.let { store.log(it) }
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                break
            }
            delay(500)
        }
    }

    Box(Modifier.fillMaxSize().background(Av.Background).statusBarsPadding()) {
        when (focus.phase) {
            FocusPhase.FOCUSING, FocusPhase.PAUSED -> Running(focus, now, onEnd = {
                if (focus.elapsed() < 60_000) focus.close() else focus.complete()?.let { store.log(it) }
            })
            FocusPhase.FINISHED -> Finished(store, focus)
            FocusPhase.ON_BREAK -> OnBreak(store, focus, now)
            FocusPhase.IDLE -> Unit
        }
    }
}

@Composable
private fun Running(focus: FocusEngine, now: Long, onEnd: () -> Unit) {
    val progress = focus.progress(now)
    val paused = focus.phase == FocusPhase.PAUSED
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("FOCUSING ON", color = Av.Muted, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 24.dp))
        Text(focus.label, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, maxLines = 2)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.size(280.dp).semantics { contentDescription = "${focus.remaining(now) / 60_000} minutes remaining" },
                contentAlignment = Alignment.Center) {
                ProgressRing(progress, Modifier.fillMaxSize(), 16.dp, if (paused) Av.Muted else Av.Accent)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(clock(focus.remaining(now)), fontSize = 64.sp, fontWeight = FontWeight.Bold)
                    Text(if (paused) "Paused" else encouragement(progress), color = Av.Muted)
                }
            }
        }
        Text("Phone face-down. Notifications can wait.", color = Av.Muted, fontSize = 13.sp)
        PrimaryButton(if (paused) "Resume" else "Pause", { if (paused) focus.resume() else focus.pause() },
            icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause)
        HoldToEndButton(onEnd)
    }
}

/** Goal-gradient copy: the closer you are, the more it pulls you to the finish. */
private fun encouragement(p: Float) = when {
    p < 0.15f -> "Settle in. Just this one thing."
    p < 0.5f -> "You're in the zone."
    p < 0.75f -> "Past halfway — keep going."
    else -> "Final stretch!"
}

/** Peak-End Rule: the last moment of every session is a win, then a 30-second recall card. */
@Composable
private fun Finished(store: AvenorStore, focus: FocusEngine) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var markDone by remember { mutableStateOf(false) }
    val session = focus.lastSession
    val reached = session?.reachedGoal ?: false
    val minutesDone = session?.focusedMinutes ?: 0
    val task = store.task(focus.taskId)

    fun wrapUp() {
        if (question.isNotBlank() && answer.isNotBlank()) {
            // First review lands tomorrow, right when forgetting starts.
            store.addCard(Flashcard(classId = task?.classId, front = question.trim(), back = answer.trim(),
                dueAt = LocalDate.now().plusDays(1).startMillis()))
        }
        if (markDone && task != null && !task.completed) store.toggle(task)
        question = ""; answer = ""; markDone = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Icon(if (reached) Icons.Filled.Verified else Icons.Filled.Spa, null, tint = if (reached) Av.Green else Av.Accent,
            modifier = Modifier.padding(top = 24.dp).size(76.dp))
        Text(if (reached) "Session complete!" else "Nice — that still counts.", fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(if (reached) "$minutesDone focused minutes on ${focus.label}." else "$minutesDone focused minutes is real progress. Shorter rounds are fine.",
            color = Av.Muted, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${store.todayFocusMinutes}m", "today", Av.Accent)
            StatTile("${store.streak}", "day streak", Av.Orange)
            StatTile("${(store.dailyGoalProgress.coerceAtMost(9.99f) * 100).toInt()}%", "daily goal", Av.Green)
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Psychology, null, tint = Av.Accent); Text("  Lock it in · 30 seconds", fontWeight = FontWeight.SemiBold)
            }
            Text("Write one question you can now answer. Avenor will quiz you on it over the next few days so it actually sticks.",
                color = Av.Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
            OutlinedTextField(question, { question = it }, label = { Text("Question (e.g. What does mitochondria do?)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(answer, { answer = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        if (task != null && !task.completed) Panel { LabeledSwitch("Mark “${task.title}” as done", markDone) { markDone = it } }
        PrimaryButton("Take a ${focus.suggestedBreakMinutes}-min break", { wrapUp(); focus.startBreak(focus.suggestedBreakMinutes) }, icon = Icons.Filled.Coffee)
        SecondaryButton("Done for now", { wrapUp(); focus.close() })
    }
}

@Composable
private fun OnBreak(store: AvenorStore, focus: FocusEngine, now: Long) {
    val left = ((focus.breakEndsAt ?: now) - now).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Break time", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
        Text(if (left > 0) clock(left) else "Ready when you are", fontSize = if (left > 0) 44.sp else 26.sp, fontWeight = FontWeight.Bold)
        // Survey: about 1 in 4 students asked for something calming.
        BreathingReset()
        Panel {
            Text("•  Stand up and stretch\n•  Drink some water\n•  Look at something far away", fontSize = 15.sp)
        }
        Text("Skip the short videos — they make it much harder to start again.", color = Av.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        PrimaryButton("Start next round", {
            val planned = (focus.plannedMillis / 60_000).toInt()
            focus.nextRound(if (planned <= 5) store.profile.defaultSessionMinutes else planned)
        }, icon = Icons.Filled.PlayArrow)
        SecondaryButton("End for now", { focus.close() })
    }
}

/** 60-second box breathing (4 in, 4 hold, 4 out, 4 hold). Starts only when tapped. */
@Composable
fun BreathingReset() {
    var startedAt by remember { mutableStateOf<Long?>(null) }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startedAt) {
        val start = startedAt ?: return@LaunchedEffect
        while (true) {
            withInfiniteAnimationFrameMillis { elapsed = System.currentTimeMillis() - start }
            if (elapsed > 64_000) break
        }
    }
    Panel {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (startedAt == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Air, null, tint = Av.Sky); Text("  60-second breathing reset", fontWeight = FontWeight.SemiBold)
                }
                Text("Slow breathing helps you settle before the next round.", color = Av.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                SecondaryButton("Start", { elapsed = 0; startedAt = System.currentTimeMillis() }, icon = Icons.Filled.PlayArrow)
            } else {
                val step = ((elapsed / 4000) % 4).toInt()
                val within = (elapsed % 4000) / 4000f
                val scale = when (step) { 0 -> 0.6f + 0.4f * within; 1 -> 1f; 2 -> 1f - 0.4f * within; else -> 0.6f }
                val words = listOf("Breathe in", "Hold", "Breathe out", "Hold")
                val label = if (elapsed >= 64_000) "Nice. Ready when you are." else words[step]
                Box(Modifier.size(140.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(140.dp).clip(CircleShape).background(Av.Sky.copy(alpha = 0.15f)))
                    Box(Modifier.size(140.dp * scale).clip(CircleShape).background(Av.Sky.copy(alpha = 0.55f)))
                }
                Text(label, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { startedAt = null }, modifier = Modifier.heightIn(min = Av.MinTap)) { Text("Stop") }
            }
        }
    }
}

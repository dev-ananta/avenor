package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorClass
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.Flashcard
import com.example.avenor.data.GradeLevel
import com.example.avenor.ui.components.LabeledSwitch
import com.example.avenor.ui.components.Panel
import com.example.avenor.ui.components.PrimaryButton
import com.example.avenor.ui.theme.Av

private val suggestions = listOf("Math", "English", "Biology", "Chemistry", "Physics", "History", "Spanish", "Computer Science")

/** Three steps, one decision each, answers pre-filled (Tesler's Law). Same flow as iOS OnboardingView. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(store: AvenorStore, onRequestReminders: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(GradeLevel.G9) }
    var goal by remember { mutableIntStateOf(60) }
    var classInput by remember { mutableStateOf("") }

    fun addClass(n: String) {
        val clean = n.trim()
        if (clean.isNotEmpty() && store.classes.none { it.name.equals(clean, true) })
            store.addClass(AvenorClass(name = clean, colorIndex = store.classes.size))
    }

    fun finish() {
        if (store.cards.isEmpty()) {
            store.addCard(Flashcard(front = "What are the two study habits that make memories last?",
                back = "Testing yourself (retrieval practice) and spacing reviews out over days. Avenor's Review tab does both for you."))
        }
        store.updateProfile {
            it.copy(name = name.trim(), grade = grade, dailyGoalMinutes = goal,
                defaultSessionMinutes = if (grade == GradeLevel.G8) 20 else 25, onboarded = true)
        }
    }

    Column(Modifier.fillMaxSize().background(Av.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i -> Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i <= step) Av.Accent else Av.Elevated)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (step) {
                0 -> {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Av.Accent, modifier = Modifier.size(44.dp))
                    Text("Welcome to Avenor", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Your planner, focus timer and memory coach — free, private, and built by students.", color = Av.Muted, fontSize = 19.sp)
                    OutlinedTextField(name, { name = it }, label = { Text("What should we call you?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("What grade are you in?", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradeLevel.entries.forEach { level ->
                            OptionTile(level.label, grade == level) { grade = level; goal = level.suggestedDailyGoal }
                        }
                    }
                }
                1 -> {
                    Text("What are you taking?", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to add. Each class gets its own color so you can spot it anywhere.", color = Av.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        suggestions.forEach { subject ->
                            val existing = store.classes.firstOrNull { it.name.equals(subject, true) }
                            OptionTile(subject, existing != null) { if (existing != null) store.deleteClass(existing.id) else addClass(subject) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(classInput, { classInput = it }, label = { Text("Add another class") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addClass(classInput); classInput = "" }))
                        IconButton(onClick = { addClass(classInput); classInput = "" }) { Icon(Icons.Filled.Add, contentDescription = "Add class") }
                    }
                    store.classes.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Av.classColor(c.colorIndex)))
                            Text("  ${c.name}", modifier = Modifier.weight(1f))
                            IconButton(onClick = { store.deleteClass(c.id) }) { Icon(Icons.Filled.Close, contentDescription = "Remove ${c.name}", tint = Av.Muted) }
                        }
                    }
                    Text("You can skip this and add classes later.", color = Av.Muted, fontSize = 13.sp)
                }
                else -> {
                    Text("How much focus a day?", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Pick something easy to hit. You can change it anytime.", color = Av.Muted)
                    listOf(30 to "One or two sessions. Great for building the habit.", 60 to "About two solid sessions a day.",
                        90 to "Three sessions — strong for busy high schoolers.", 120 to "Four-plus sessions — college-level study load.").forEach { (m, blurb) ->
                        val sel = goal == m
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(Av.RadiusM))
                                .background(if (sel) Av.Accent else Av.Surface).clickable { goal = m }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(when (m) { 30 -> "30 minutes"; 60 -> "1 hour"; 90 -> "1½ hours"; else -> "2 hours" },
                                    fontWeight = FontWeight.SemiBold, color = if (sel) Color.Black else Av.Text)
                                Text(blurb, fontSize = 12.sp, color = if (sel) Color.Black.copy(alpha = 0.7f) else Av.Muted)
                            }
                            if (m == grade.suggestedDailyGoal) Text("Suggested", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.Black else Av.Accent)
                        }
                    }
                    Panel {
                        Text("Turn on reminders?", fontWeight = FontWeight.SemiBold)
                        Text("Avenor reminds you the night before something is due and when a focus session ends. Nothing else, ever.",
                            color = Av.Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))
                        LabeledSwitch("Reminders", store.profile.remindersEnabled) { on ->
                            if (on) onRequestReminders() else store.updateProfile { it.copy(remindersEnabled = false) }
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton(if (step == 2) "Start studying" else "Continue", { if (step < 2) step += 1 else finish() })
            if (step > 0) TextButton(onClick = { step -= 1 }, modifier = Modifier.fillMaxWidth().heightIn(min = Av.MinTap)) { Text("Back", color = Av.Muted) }
        }
    }
}

@Composable
private fun OptionTile(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(Av.RadiusS))
            .background(if (selected) Av.Accent else Av.Surface).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Text(if (selected) "  $text" else text, fontWeight = FontWeight.SemiBold, color = if (selected) Color.Black else Av.Text)
    }
}

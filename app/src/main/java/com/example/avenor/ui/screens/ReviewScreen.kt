package com.example.avenor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.Flashcard
import com.example.avenor.ui.components.Chip
import com.example.avenor.ui.components.ChoiceRow
import com.example.avenor.ui.components.EditorSheet
import com.example.avenor.ui.components.EmptyHint
import com.example.avenor.ui.components.MasteryDots
import com.example.avenor.ui.components.Panel
import com.example.avenor.ui.components.PrimaryButton
import com.example.avenor.ui.components.SecondaryButton
import com.example.avenor.ui.components.StatTile
import com.example.avenor.ui.components.classColor
import com.example.avenor.ui.theme.Av
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.example.avenor.domain.toLocalDate

private fun relativeDay(millis: Long): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), millis.toLocalDate())
    return when {
        days <= 0 -> "later today"
        days == 1L -> "tomorrow"
        else -> "in $days days"
    }
}

/** Retrieval practice + spacing. Cards return after 1, 2, 4, 8 and 16 days. */
@Composable
fun ReviewScreen(store: AvenorStore, onStartReview: () -> Unit) {
    var editing by remember { mutableStateOf<Flashcard?>(null) }
    var creating by remember { mutableStateOf(false) }
    val due = store.dueCards.size
    val groups = store.classes.mapNotNull { c ->
        store.cards.filter { it.classId == c.id }.sortedBy { it.dueAt }.takeIf { it.isNotEmpty() }?.let { Triple(c.name, Av.classColor(c.colorIndex), it) }
    } + listOfNotNull(store.cards.filter { store.classInfo(it.classId) == null }.sortedBy { it.dueAt }.takeIf { it.isNotEmpty() }
        ?.let { Triple("General", Av.Muted, it) })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Review", fontSize = 30.sp, fontWeight = FontWeight.Bold) }
            item {
                // Von Restorff: bright only when there's something to do.
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Av.RadiusXL))
                        .then(if (due > 0) Modifier.background(Av.HeroGradient) else Modifier.background(Av.Surface))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (due > 0) {
                        Text("READY TO REVIEW", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp)
                        Text(if (due == 1) "1 card" else "$due cards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                        Text(if (due > 20) "We'll do 20 now — small sets stick better." else "About ${maxOf(1, due / 4)} min. Answer in your head before you flip.",
                            color = Color.White.copy(alpha = 0.85f))
                        PrimaryButton("Start review", onStartReview, icon = Icons.Filled.PlayArrow, container = Color.White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, null, tint = Av.Green); Text("  All caught up", color = Av.Green, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Text(store.nextCardDue?.let { "Next card comes back ${relativeDay(it)}." } ?: "Add cards from your notes or after a focus session.", color = Av.Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("${store.reviewsToday}", "reviewed today", Av.Violet)
                            StatTile("${store.cards.count { it.box >= 4 }}", "mastered", Av.Green)
                        }
                    }
                }
            }
            if (store.cards.isEmpty()) {
                item { Panel { EmptyHint(Icons.Filled.AutoAwesome, "Your memory bank is empty",
                    "Finish a focus session and write one question you can answer — or tap + to add a card. Keep each card to one idea.") } }
            }
            groups.forEach { (title, color, list) ->
                item(key = "g-$title") {
                    Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text("  ${title.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Av.Muted, modifier = Modifier.weight(1f))
                        Text("${list.size}", fontSize = 12.sp, color = Av.Muted)
                    }
                }
                items(list, key = { it.id }) { card ->
                    Panel(Modifier.clickable { editing = card }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(card.front, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                val isDue = card.dueAt <= System.currentTimeMillis()
                                Text(if (isDue) "Due now" else "Next: ${relativeDay(card.dueAt)}", fontSize = 12.sp, color = if (isDue) Av.Accent else Av.Muted)
                            }
                            MasteryDots(card.box, color)
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, containerColor = Av.Accent, contentColor = Color.Black,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Filled.Add, contentDescription = "Add card") }
    }
    if (creating) CardEditor(store, null) { creating = false }
    editing?.let { CardEditor(store, it) { editing = null } }
}

/** Full-screen review set. Hick's Law: two honest answers, not a 4-level difficulty scale. */
@Composable
fun ReviewSessionScreen(store: AvenorStore, initial: List<Flashcard>, onClose: () -> Unit) {
    var queue by remember { mutableStateOf(initial) }
    val total = remember { initial.size }
    var revealed by remember { mutableStateOf(false) }
    var firstTry by remember { mutableIntStateOf(0) }
    var missed by remember { mutableStateOf(setOf<String>()) }
    val haptics = LocalHapticFeedback.current

    fun grade(card: Flashcard, remembered: Boolean) {
        val relearning = card.id in missed
        store.review(card.id, remembered, relearning)
        queue = queue.drop(1)
        if (remembered) { if (!relearning) firstTry += 1 } else { missed = missed + card.id; queue = queue + card }
        revealed = false
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(Modifier.fillMaxSize().background(Av.Background).statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        val card = queue.firstOrNull()
        if (card != null) {
            val done = total - queue.size
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close review") }
                LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.weight(1f), color = Av.Accent, trackColor = Av.Elevated)
                Text("$done/$total", color = Av.Muted)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 300.dp).clip(RoundedCornerShape(28.dp)).background(Av.Surface)
                        .border(1.dp, Av.Border, RoundedCornerShape(28.dp)).clickable { revealed = true }.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                ) {
                    store.classInfo(card.classId)?.let { Chip(it.name, Av.classColor(it.colorIndex)) }
                    Text(card.front, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (revealed) Text(card.back, fontSize = 19.sp, textAlign = TextAlign.Center, color = Av.Text)
                }
            }
            if (revealed) {
                Text("Did you remember it?", color = Av.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Not yet", { grade(card, false) }, Modifier.weight(1f), Icons.Filled.Refresh)
                    PrimaryButton("Got it", { grade(card, true) }, Modifier.weight(1f), Icons.Filled.Check, container = Av.Green)
                }
            } else {
                Text("Answer in your head first — the effort is what makes it stick.", color = Av.Muted, fontSize = 13.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                PrimaryButton("Show answer", { revealed = true })
            }
        } else {
            // Peak-End: finish on what you DID remember, and frame misses as normal.
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = Av.Gold, modifier = Modifier.size(72.dp))
                    Text("Review complete!", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(if (total == 0) "Nothing was due." else "You remembered $firstTry of $total on the first try.", fontSize = 19.sp, textAlign = TextAlign.Center)
                    if (missed.isNotEmpty()) Text("The ${missed.size} you missed will come back tomorrow. Forgetting and re-learning is exactly how memories get stronger.",
                        color = Av.Muted, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("${store.reviewsToday}", "reviews today", Av.Violet)
                        StatTile("${store.streak}", "day streak", Av.Orange)
                    }
                }
            }
            PrimaryButton("Done", onClose)
        }
    }
}

@Composable
private fun CardEditor(store: AvenorStore, card: Flashcard?, onDismiss: () -> Unit) {
    var front by remember { mutableStateOf(card?.front ?: "") }
    var back by remember { mutableStateOf(card?.back ?: "") }
    var classId by remember { mutableStateOf(card?.classId) }
    var error by remember { mutableStateOf("") }
    EditorSheet(if (card == null) "New Card" else "Edit Card", onDismiss, onSave = {
        if (front.isBlank() || back.isBlank()) {
            error = "Add both a question and an answer."
        } else {
            if (card == null) store.addCard(Flashcard(classId = classId, front = front.trim(), back = back.trim()))
            else store.updateCard(card.copy(front = front.trim(), back = back.trim(), classId = classId))
            onDismiss()
        }
    }) {
        OutlinedTextField(front, { front = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(back, { back = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
        Text("One idea per card. Short cards are faster to review and easier to remember.", color = Av.Muted, fontSize = 13.sp)
        if (store.classes.isNotEmpty()) {
            ChoiceRow(listOf<String?>(null) + store.classes.map { it.id }, classId, { store.classInfo(it)?.name ?: "None" }) { classId = it }
        }
        if (card != null) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Mastery  ", color = Av.Muted); MasteryDots(card.box, store.classColor(card.classId)) }
            TextButton(onClick = { store.deleteCard(card.id); onDismiss() }) { Text("Delete card", color = Av.Red) }
        }
        if (error.isNotEmpty()) Text(error, color = Av.Red)
    }
}

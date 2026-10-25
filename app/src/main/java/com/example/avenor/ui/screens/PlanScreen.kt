package com.example.avenor.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorClass
import com.example.avenor.data.AvenorEvent
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.AvenorTask
import com.example.avenor.domain.FocusEngine
import com.example.avenor.domain.Planner
import com.example.avenor.domain.TaskBucket
import com.example.avenor.ui.components.ChoiceRow
import com.example.avenor.ui.components.EditorSheet
import com.example.avenor.ui.components.EmptyHint
import com.example.avenor.ui.components.LabeledSwitch
import com.example.avenor.ui.components.Panel
import com.example.avenor.ui.components.QuickAddBar
import com.example.avenor.ui.components.SecondaryButton
import com.example.avenor.ui.components.TaskEditor
import com.example.avenor.ui.components.TaskRow
import com.example.avenor.ui.components.classColor
import com.example.avenor.ui.theme.Av
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class PlanSegment(val label: String) { TASKS("Tasks"), CALENDAR("Calendar"), CLASSES("Classes") }

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

fun eventTimeLabel(e: AvenorEvent): String = when {
    e.hasTime -> "${e.start.format(timeFormat)} – ${e.start.plusMinutes(e.durationMinutes.toLong()).format(timeFormat)}"
    e.legacyTime.isNotBlank() -> e.legacyTime
    else -> "All day"
}

/** Hick's Law: Tasks, Calendar and Classes share one tab behind a standard segmented control (Jakob's Law). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(store: AvenorStore, focus: FocusEngine, snackbar: SnackbarHostState) {
    var segment by rememberSaveable { mutableStateOf(PlanSegment.TASKS) }
    Column(Modifier.fillMaxSize()) {
        Text("Plan", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            PlanSegment.entries.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = segment == s,
                    onClick = { segment = s },
                    shape = SegmentedButtonDefaults.itemShape(i, PlanSegment.entries.size),
                ) { Text(s.label) }
            }
        }
        when (segment) {
            PlanSegment.TASKS -> TaskList(store, focus, snackbar)
            PlanSegment.CALENDAR -> CalendarView(store)
            PlanSegment.CLASSES -> ClassesView(store)
        }
    }
}

// ---------- Tasks ----------

@Composable
private fun TaskList(store: AvenorStore, focus: FocusEngine, snackbar: SnackbarHostState) {
    var editing by remember { mutableStateOf<AvenorTask?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val active = store.rankedActive
    // Miller's Law: chunk one long list into small, meaningful groups.
    val groups = TaskBucket.entries.mapNotNull { b -> active.filter { Planner.bucket(it) == b }.takeIf { it.isNotEmpty() }?.let { b to it } }
    val completed = store.tasks.filter { it.completed }.sortedByDescending { it.completedAt ?: 0L }

    // Undo instead of "Are you sure?" (Material snackbar pattern)
    val deleted = store.recentlyDeleted
    LaunchedEffect(deleted?.id) {
        if (deleted != null) {
            val result = snackbar.showSnackbar("Task deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) store.undoDelete() else store.recentlyDeleted = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (groups.isEmpty() && completed.isEmpty()) {
                item {
                    Panel {
                        EmptyHint(Icons.Filled.Checklist, "No tasks yet",
                            "Type one below. Add a day, a time and “!” for important — e.g. “Chem worksheet tmrw 30m !”.")
                    }
                }
            }
            groups.forEach { (bucket, bucketTasks) ->
                item(key = "h-${bucket.name}") {
                    Row(Modifier.padding(top = 10.dp, start = 4.dp)) {
                        Text(bucket.label.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                            color = if (bucket == TaskBucket.OVERDUE) Av.Red else Av.Muted, modifier = Modifier.weight(1f))
                        Text("${bucketTasks.size}", fontSize = 12.sp, color = Av.Muted)
                    }
                }
                items(bucketTasks, key = { it.id }) { task ->
                    Box(Modifier.clip(RoundedCornerShape(Av.RadiusM)).background(Av.Surface).padding(horizontal = 4.dp)) {
                        TaskRow(store, task, onToggle = { store.toggle(task) }, onOpen = { editing = task },
                            onFocus = { focus.start(task.id, task.title, store.profile.defaultSessionMinutes) })
                    }
                }
            }
            if (completed.isNotEmpty()) {
                item(key = "completed-toggle") {
                    TextButton(onClick = { showCompleted = !showCompleted }, modifier = Modifier.heightIn(min = Av.MinTap)) {
                        Text("${if (showCompleted) "Hide" else "Show"} completed (${completed.size})", color = Av.Muted)
                    }
                }
                if (showCompleted) {
                    items(completed.take(50), key = { "c-" + it.id }) { task ->
                        Box(Modifier.clip(RoundedCornerShape(Av.RadiusM)).background(Av.Surface).padding(horizontal = 4.dp)) {
                            TaskRow(store, task, onToggle = { store.toggle(task) }, onOpen = { editing = task },
                                onFocus = { focus.start(task.id, task.title, store.profile.defaultSessionMinutes) })
                        }
                    }
                }
            }
        }
        QuickAddBar(store, Modifier.align(Alignment.BottomCenter).background(Av.Background).padding(horizontal = 16.dp, vertical = 8.dp))
    }
    editing?.let { TaskEditor(store, it) { editing = null } }
}

// ---------- Calendar ----------

@Composable
private fun CalendarView(store: AvenorStore) {
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var newEvent by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AvenorEvent?>(null) }
    var editingTask by remember { mutableStateOf<AvenorTask?>(null) }
    val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(weekOffset.toLong())
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val dayEvents = store.schedule(selected)
    val dayTasks = store.tasksDue(selected)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(weekStart.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { weekOffset -= 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week") }
                    TextButton(onClick = { weekOffset = 0; selected = LocalDate.now() }) { Text("Today") }
                    IconButton(onClick = { weekOffset += 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.forEach { day ->
                        val isSel = day == selected
                        val hasItems = store.schedule(day).isNotEmpty() || store.tasksDue(day).any { !it.completed }
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 64.dp)
                                .clip(RoundedCornerShape(Av.RadiusS))
                                .background(if (isSel) Av.Accent else Av.Surface)
                                .clickable { selected = day }
                                .semantics { contentDescription = day.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")); this.selected = isSel }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), fontSize = 11.sp,
                                color = if (isSel) Color.Black.copy(alpha = 0.7f) else Av.Muted)
                            Text("${day.dayOfMonth}", fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else if (day == LocalDate.now()) Av.Accent else Av.Text)
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (!hasItems) Color.Transparent else if (isSel) Color.Black else Av.Accent))
                        }
                    }
                }
            }
            item {
                Text(selected.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = Av.Muted, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                item { Panel { Text("Nothing scheduled. Tap + to add a class, test, or practice.", color = Av.Muted) } }
            }
            items(dayEvents, key = { it.id }) { e ->
                Panel(Modifier.clickable { editingEvent = e }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(4.dp).height(38.dp).clip(RoundedCornerShape(2.dp)).background(store.classColor(e.classId)))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(e.title, fontWeight = FontWeight.SemiBold)
                            Text(eventTimeLabel(e), color = Av.Muted, fontSize = 14.sp)
                        }
                    }
                }
            }
            items(dayTasks, key = { "t-" + it.id }) { t ->
                Box(Modifier.clip(RoundedCornerShape(Av.RadiusM)).background(Av.Surface).padding(horizontal = 4.dp)) {
                    TaskRow(store, t, onToggle = { store.toggle(t) }, onOpen = { editingTask = t })
                }
            }
        }
        FloatingActionButton(
            onClick = { newEvent = true },
            containerColor = Av.Accent, contentColor = Color.Black,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Add event") }
    }
    if (newEvent) EventEditor(store, null, selected) { newEvent = false }
    editingEvent?.let { EventEditor(store, it, it.start.toLocalDate()) { editingEvent = null } }
    editingTask?.let { TaskEditor(store, it) { editingTask = null } }
}

@Composable
private fun EventEditor(store: AvenorStore, event: AvenorEvent?, day: LocalDate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val defaultStart = day.atTime((LocalDateTime.now().hour + 1) % 24, 0)
    var title by remember { mutableStateOf(event?.title ?: "") }
    var start by remember { mutableStateOf(event?.start ?: defaultStart) }
    var hasTime by remember { mutableStateOf(event?.hasTime ?: true) }
    var duration by remember { mutableIntStateOf(event?.durationMinutes ?: 60) }
    var classId by remember { mutableStateOf(event?.classId) }
    var error by remember { mutableStateOf("") }

    EditorSheet(if (event == null) "New Event" else "Edit Event", onDismiss, onSave = {
        val clean = title.trim()
        if (clean.isEmpty()) {
            error = "Give the event a title."
        } else {
            val updated = (event ?: AvenorEvent(title = clean, start = start)).copy(
                title = clean, start = if (hasTime) start else start.toLocalDate().atStartOfDay(), hasTime = hasTime,
                durationMinutes = duration, classId = classId, legacyTime = if (hasTime) "" else (event?.legacyTime ?: ""),
            )
            if (event == null) store.addEvent(updated) else store.updateEvent(updated)
            onDismiss()
        }
    }) {
        OutlinedTextField(title, { title = it }, label = { Text("Title (e.g. Bio test, practice)") }, modifier = Modifier.fillMaxWidth())
        if (store.classes.isNotEmpty()) {
            Text("Class", color = Av.Muted, fontSize = 13.sp)
            ChoiceRow(listOf<String?>(null) + store.classes.map { it.id }, classId, { store.classInfo(it)?.name ?: "None" }) { classId = it }
        }
        SecondaryButton(start.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, MMM d")), onClick = {
            DatePickerDialog(context, { _, y, m, d -> start = start.withYear(y).withMonth(m + 1).withDayOfMonth(d) },
                start.year, start.monthValue - 1, start.dayOfMonth).show()
        }, icon = Icons.Filled.CalendarToday)
        LabeledSwitch("Specific time", hasTime) { hasTime = it }
        if (hasTime) {
            SecondaryButton(start.format(timeFormat), onClick = {
                TimePickerDialog(context, { _, h, min -> start = start.withHour(h).withMinute(min) }, start.hour, start.minute, false).show()
            }, icon = Icons.Filled.Schedule)
            Text("Length", color = Av.Muted, fontSize = 13.sp)
            ChoiceRow(listOf(15, 30, 45, 60, 90, 120, 180), duration, { if (it < 60) "$it min" else if (it % 60 == 0) "${it / 60} hr" else "${it / 60} hr ${it % 60}" }) { duration = it }
        }
        if (event != null && !event.hasTime && event.legacyTime.isNotBlank()) Text("Saved time: ${event.legacyTime}", color = Av.Muted)
        if (error.isNotEmpty()) Text(error, color = Av.Red)
        if (event != null) TextButton(onClick = { store.deleteEvent(event.id); onDismiss() }) { Text("Delete event", color = Av.Red) }
    }
}

// ---------- Classes ----------

@Composable
private fun ClassesView(store: AvenorStore) {
    var editing by remember { mutableStateOf<AvenorClass?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AvenorClass?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (store.classes.isEmpty()) {
                item { Panel { EmptyHint(Icons.Filled.MenuBook, "No classes yet", "Add your classes so tasks, tests and review cards get color-coded automatically.") } }
            }
            items(store.classes, key = { it.id }) { c ->
                val color = Av.classColor(c.colorIndex)
                val open = store.tasks.count { it.classId == c.id && !it.completed }
                val cardCount = store.cards.count { it.classId == c.id }
                Panel(Modifier.clickable { editing = c }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = color,
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.18f)).padding(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            val detail = listOf(c.teacher, if (c.room.isBlank()) "" else "Room ${c.room}").filter { it.isNotBlank() }.joinToString(" · ")
                            if (detail.isNotEmpty()) Text(detail, color = Av.Muted, fontSize = 14.sp)
                            Text("$open open ${if (open == 1) "task" else "tasks"} · $cardCount ${if (cardCount == 1) "card" else "cards"}", color = Av.Muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { pendingDelete = c }) { Icon(Icons.Filled.Close, contentDescription = "Delete ${c.name}", tint = Av.Muted) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, containerColor = Av.Accent, contentColor = Color.Black,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Filled.Add, contentDescription = "Add class") }
    }
    if (creating) ClassEditor(store, null) { creating = false }
    editing?.let { ClassEditor(store, it) { editing = null } }
    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${c.name}?") },
            text = { Text("Its tasks and review cards are kept — they just won't be filed under a class.") },
            confirmButton = { TextButton(onClick = { store.deleteClass(c.id); pendingDelete = null }) { Text("Delete", color = Av.Red) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            containerColor = Av.Elevated,
        )
    }
}

@Composable
fun ClassEditor(store: AvenorStore, item: AvenorClass?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var teacher by remember { mutableStateOf(item?.teacher ?: "") }
    var room by remember { mutableStateOf(item?.room ?: "") }
    var colorIndex by remember { mutableIntStateOf(item?.colorIndex ?: (store.classes.size % Av.ClassPalette.size)) }
    var error by remember { mutableStateOf("") }
    EditorSheet(if (item == null) "New Class" else "Edit Class", onDismiss, onSave = {
        val clean = name.trim()
        if (clean.isEmpty()) {
            error = "Enter a class name."
        } else {
            if (item == null) store.addClass(AvenorClass(name = clean, teacher = teacher, room = room, colorIndex = colorIndex))
            else store.updateClass(item.copy(name = clean, teacher = teacher, room = room, colorIndex = colorIndex))
            onDismiss()
        }
    }) {
        OutlinedTextField(name, { name = it }, label = { Text("Class name (e.g. Biology)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(teacher, { teacher = it }, label = { Text("Teacher") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(room, { room = it }, label = { Text("Room") }, modifier = Modifier.fillMaxWidth())
        Text("Color", color = Av.Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Av.ClassPalette.forEachIndexed { i, color ->
                Box(
                    Modifier
                        .size(Av.MinTap)
                        .clickable { colorIndex = i }
                        .semantics { contentDescription = "Color ${i + 1}"; selected = colorIndex == i },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(color)
                            .then(if (colorIndex == i) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                    )
                }
            }
        }
        if (error.isNotEmpty()) Text(error, color = Av.Red)
        if (item != null) HorizontalDivider(color = Av.Border)
    }
}

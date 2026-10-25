package com.example.avenor.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.data.AvenorStore
import com.example.avenor.data.AvenorTask
import com.example.avenor.data.Priority
import com.example.avenor.domain.Planner
import com.example.avenor.domain.QuickAddParser
import com.example.avenor.domain.TaskBucket
import com.example.avenor.ui.theme.Av
import java.time.LocalDate

fun AvenorStore.classColor(id: String?): Color = classInfo(id)?.let { Av.classColor(it.colorIndex) } ?: Av.Muted

/**
 * Tesler's Law + Fitts's Law: one field pinned in the thumb zone that understands "Bio essay fri 45m !".
 */
@Composable
fun QuickAddBar(store: AvenorStore, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val parsed = remember(text, store.classes) { QuickAddParser.parse(text, store.classes) }

    fun add() {
        val title = parsed.title.trim()
        if (title.isEmpty()) return
        store.addTask(
            AvenorTask(
                title = title, className = parsed.className ?: "", classId = parsed.classId, dueDate = parsed.dueDate,
                priority = parsed.priority ?: Priority.MEDIUM,
                estimatedMinutes = parsed.minutes ?: store.profile.defaultSessionMinutes,
            )
        )
        text = ""
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (text.isNotBlank()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(parsed.title.ifBlank { "Add a title" }, color = Av.Text)
                parsed.dueDate?.let { Chip(Planner.dueLabel(it), Av.Accent, Icons.Filled.CalendarToday) }
                parsed.minutes?.let { Chip("$it min", Av.Sky, Icons.Filled.Schedule) }
                parsed.priority?.let { Chip(it.label, priorityColor(it), Icons.Filled.Flag) }
                parsed.className?.let { Chip(it, store.classColor(parsed.classId), Icons.Filled.School) }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(Av.Elevated, RoundedCornerShape(18.dp))
                .border(1.dp, Av.Border, RoundedCornerShape(18.dp))
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Av.Accent)
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Add a task — try “Bio essay fri 45m !”", color = Av.Muted, maxLines = 1) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            if (text.isEmpty()) {
                IconButton(onClick = { showForm = true }) { Icon(Icons.Filled.Tune, contentDescription = "Add task with details") }
            } else {
                IconButton(onClick = { add() }) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Add task", tint = Av.Accent) }
            }
        }
    }
    if (showForm) TaskEditor(store, task = null, onDismiss = { showForm = false })
}

fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> Av.Red
    Priority.MEDIUM -> Av.Orange
    Priority.LOW -> Av.Green
}

/** Task row with a 48 dp checkbox, tap-to-edit, and an overflow menu (Material pattern, Jakob's Law). */
@Composable
fun TaskRow(
    store: AvenorStore,
    task: AvenorTask,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onFocus: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val overdue = !task.completed && Planner.bucket(task) == TaskBucket.OVERDUE
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit task") { onOpen() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(Av.MinTap)) {
            Icon(
                if (task.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (task.completed) "Mark ${task.title} incomplete" else "Mark ${task.title} complete",
                tint = if (task.completed) Av.Green else Av.Muted,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                task.title, fontWeight = FontWeight.SemiBold, maxLines = 2,
                color = if (task.completed) Av.Muted else Av.Text,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val name = store.className(task)
                if (name.isNotEmpty()) Chip(name, store.classColor(task.classId))
                if (!task.completed) {
                    task.dueDate?.let { Chip(Planner.dueLabel(it), if (overdue) Av.Red else Av.Muted, Icons.Filled.CalendarToday) }
                    Chip("${task.estimatedMinutes}m", icon = Icons.Filled.Schedule)
                }
            }
        }
        if (task.priority == Priority.HIGH && !task.completed) {
            Icon(Icons.Filled.Error, contentDescription = "High priority", tint = Av.Red, modifier = Modifier.size(20.dp))
        }
        if (onFocus != null) {
            Column {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (!task.completed) DropdownMenuItem(text = { Text("Start focus session") }, onClick = { menu = false; onFocus() })
                    DropdownMenuItem(text = { Text(if (task.completed) "Mark not done" else "Mark done") }, onClick = { menu = false; onToggle() })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Delete", color = Av.Red) }, onClick = { menu = false; store.deleteTask(task.id) })
                }
            }
        }
    }
}

// ---------- Editors ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Av.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(onClick = onSave) { Text("Save", fontWeight = FontWeight.Bold) }
            }
            content()
        }
    }
}

@Composable
fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = Av.MinTap), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Av.Accent, checkedThumbColor = Color.Black))
    }
}

@Composable
fun <T> ChoiceRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) },
                modifier = Modifier.heightIn(min = Av.MinTap))
        }
    }
}

@Composable
fun TaskEditor(store: AvenorStore, task: AvenorTask?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task?.title ?: "") }
    var details by remember { mutableStateOf(task?.details ?: "") }
    var classId by remember { mutableStateOf(task?.classId) }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var minutes by remember { mutableIntStateOf(task?.estimatedMinutes ?: store.profile.defaultSessionMinutes) }
    var due by remember { mutableStateOf(task?.dueDate) }
    var error by remember { mutableStateOf("") }

    EditorSheet(
        title = if (task == null) "New Task" else "Edit Task",
        onDismiss = onDismiss,
        onSave = {
            val clean = title.trim()
            if (clean.isEmpty()) {
                error = "Give the task a title."
            } else {
                val cls = store.classInfo(classId)
                val updated = (task ?: AvenorTask(title = clean)).copy(
                    title = clean, details = details, classId = classId, priority = priority, estimatedMinutes = minutes, dueDate = due,
                    className = cls?.name ?: if (task?.classId != null) "" else (task?.className ?: ""),
                )
                if (task == null) store.addTask(updated) else store.updateTask(updated)
                onDismiss()
            }
        },
    ) {
        OutlinedTextField(title, { title = it }, label = { Text("What do you need to do?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(details, { details = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        if (store.classes.isNotEmpty()) {
            Text("Class", color = Av.Muted, fontSize = 13.sp)
            ChoiceRow(listOf<String?>(null) + store.classes.map { it.id }, classId,
                label = { id -> store.classInfo(id)?.name ?: "None" }, onSelect = { classId = it })
        }
        Text("Priority", color = Av.Muted, fontSize = 13.sp)
        ChoiceRow(Priority.entries.toList(), priority, label = { it.label }, onSelect = { priority = it })
        Text("About $minutes min · ${Planner.sessions(minutes, store.profile.defaultSessionMinutes).let { if (it == 1) "1 focus session" else "$it focus sessions" }}",
            color = Av.Muted, fontSize = 13.sp)
        ChoiceRow(listOf(15, 30, 45, 60, 90, 120, 180), minutes, label = { if (it < 60) "${it}m" else "${it / 60}h${if (it % 60 == 0) "" else " ${it % 60}m"}" },
            onSelect = { minutes = it })
        LabeledSwitch("Due date", due != null) { on -> due = if (on) (due ?: LocalDate.now()) else null }
        due?.let { d ->
            SecondaryButton(Planner.dueLabel(d) + " · change", onClick = {
                DatePickerDialog(context, { _, y, m, day -> due = LocalDate.of(y, m + 1, day) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
            }, icon = Icons.Filled.CalendarToday)
        }
        if (error.isNotEmpty()) Text(error, color = Av.Red)
        if (task != null) {
            TextButton(onClick = { store.deleteTask(task.id); onDismiss() }) { Text("Delete task", color = Av.Red) }
        }
    }
}

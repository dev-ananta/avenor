package com.example.avenor.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.avenor.domain.Leitner
import com.example.avenor.domain.Planner
import com.example.avenor.domain.QuickAddParser
import com.example.avenor.domain.Streaks
import com.example.avenor.domain.toLocalDate
import com.example.avenor.notify.Reminders
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

enum class Destination(val label: String) { TODAY("Today"), PLAN("Plan"), FOCUS("Focus"), REVIEW("Review") }

/**
 * Single source of truth, stored on-device in SharedPreferences as JSON.
 * No servers, accounts or analytics — the same privacy promise as the iOS app.
 */
class AvenorStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("avenor_v3", Context.MODE_PRIVATE)

    var tasks by mutableStateOf(listOf<AvenorTask>())
        private set
    var classes by mutableStateOf(listOf<AvenorClass>())
        private set
    var events by mutableStateOf(listOf<AvenorEvent>())
        private set
    var sessions by mutableStateOf(listOf<FocusSession>())
        private set
    var cards by mutableStateOf(listOf<Flashcard>())
        private set
    var reviewLog by mutableStateOf(mapOf<LocalDate, Int>())
        private set
    var profile by mutableStateOf(StudentProfile())
        private set

    // Navigation + transient UI state
    var tab by mutableStateOf(Destination.TODAY)
    var recentlyDeleted by mutableStateOf<AvenorTask?>(null)

    init {
        if (prefs.contains("tasks") || prefs.contains("profile")) load() else migrateFromVersion01()
    }

    // ---------- Tasks ----------

    val rankedActive: List<AvenorTask> get() = Planner.ranked(tasks)

    fun task(id: String?): AvenorTask? = id?.let { i -> tasks.firstOrNull { it.id == i } }

    fun addTask(task: AvenorTask) { tasks = listOf(task) + tasks; saveTasks(); Reminders.sync(context, task, profile.remindersEnabled) }

    fun updateTask(task: AvenorTask) {
        tasks = tasks.map { if (it.id == task.id) task else it }; saveTasks()
        Reminders.sync(context, task, profile.remindersEnabled)
    }

    fun toggle(task: AvenorTask) {
        val current = task(task.id) ?: return
        val done = !current.completed
        updateTask(current.copy(completed = done, completedAt = if (done) System.currentTimeMillis() else null))
    }

    /** Deletes right away but keeps a copy for the Undo snackbar (safer and faster than a confirm dialog). */
    fun deleteTask(id: String) {
        recentlyDeleted = task(id)
        tasks = tasks.filterNot { it.id == id }; saveTasks()
        Reminders.cancelTask(context, id)
    }

    fun undoDelete() {
        val t = recentlyDeleted ?: return
        recentlyDeleted = null
        addTask(t)
    }

    fun className(task: AvenorTask): String = classInfo(task.classId)?.name ?: task.className

    fun tasksDue(day: LocalDate) = tasks.filter { it.dueDate == day }

    val tasksDoneToday: Int get() = tasks.count { it.completedAt?.toLocalDate() == LocalDate.now() }

    // ---------- Classes ----------

    fun classInfo(id: String?): AvenorClass? = id?.let { i -> classes.firstOrNull { it.id == i } }

    fun addClass(item: AvenorClass) { classes = classes + item; saveClasses() }

    fun updateClass(item: AvenorClass) {
        classes = classes.map { if (it.id == item.id) item else it }; saveClasses()
        tasks = tasks.map { if (it.classId == item.id) it.copy(className = item.name) else it }; saveTasks()
    }

    /** Keeps the class's tasks, events and cards — they just become unfiled. */
    fun deleteClass(id: String) {
        classes = classes.filterNot { it.id == id }; saveClasses()
        tasks = tasks.map { if (it.classId == id) it.copy(classId = null) else it }; saveTasks()
        cards = cards.map { if (it.classId == id) it.copy(classId = null) else it }; saveCards()
        events = events.map { if (it.classId == id) it.copy(classId = null) else it }; saveEvents()
    }

    // ---------- Events ----------

    fun addEvent(item: AvenorEvent) { events = events + item; saveEvents() }
    fun updateEvent(item: AvenorEvent) { events = events.map { if (it.id == item.id) item else it }; saveEvents() }
    fun deleteEvent(id: String) { events = events.filterNot { it.id == id }; saveEvents() }
    fun schedule(day: LocalDate) = events.filter { it.start.toLocalDate() == day }.sortedBy { it.start }

    // ---------- Focus ----------

    fun log(session: FocusSession) {
        sessions = sessions + session; saveSessions()
        session.taskId?.let { id ->
            tasks = tasks.map { if (it.id == id) it.copy(focusedMinutes = it.focusedMinutes + session.focusedMinutes) else it }
            saveTasks()
        }
    }

    fun focusMinutes(day: LocalDate) = sessions.filter { it.startedAt.toLocalDate() == day }.sumOf { it.focusedMinutes }
    val todayFocusMinutes: Int get() = focusMinutes(LocalDate.now())
    val dailyGoalProgress: Float get() = todayFocusMinutes.toFloat() / profile.dailyGoalMinutes.coerceAtLeast(1)
    val totalFocusMinutes: Int get() = sessions.sumOf { it.focusedMinutes }

    // ---------- Review ----------

    val dueCards: List<Flashcard> get() {
        val now = System.currentTimeMillis()
        return cards.filter { it.dueAt <= now }.sortedBy { it.dueAt }
    }
    val nextCardDue: Long? get() = cards.map { it.dueAt }.filter { it > System.currentTimeMillis() }.minOrNull()
    val reviewsToday: Int get() = reviewLog[LocalDate.now()] ?: 0

    fun addCard(card: Flashcard) { cards = cards + card; saveCards() }
    fun updateCard(card: Flashcard) { cards = cards.map { if (it.id == card.id) card else it }; saveCards() }
    fun deleteCard(id: String) { cards = cards.filterNot { it.id == id }; saveCards() }

    fun review(cardId: String, remembered: Boolean, relearning: Boolean) {
        cards = cards.map { if (it.id == cardId) Leitner.review(it, remembered, relearning) else it }; saveCards()
        val today = LocalDate.now()
        reviewLog = reviewLog + (today to ((reviewLog[today] ?: 0) + 1)); saveReviewLog()
    }

    // ---------- Streak ----------

    /** A day counts if you focused 5+ minutes, finished a task, or reviewed a card. */
    val activeDays: Set<LocalDate> get() = buildSet {
        sessions.filter { it.focusedSeconds >= 300 }.forEach { add(it.startedAt.toLocalDate()) }
        tasks.forEach { t -> t.completedAt?.let { add(it.toLocalDate()) } }
        reviewLog.filter { it.value > 0 }.keys.forEach { add(it) }
    }
    val streak: Int get() = Streaks.current(activeDays)

    // ---------- Profile ----------

    fun updateProfile(transform: (StudentProfile) -> StudentProfile) {
        val old = profile
        profile = transform(profile); saveProfile()
        if (old.remindersEnabled != profile.remindersEnabled || old.planReminderHour != profile.planReminderHour) resyncReminders()
    }

    fun resyncReminders() = Reminders.resyncAll(context, tasks, profile)

    fun resetAll() {
        Reminders.cancelAll(context, tasks)
        tasks = emptyList(); classes = emptyList(); events = emptyList(); sessions = emptyList()
        cards = emptyList(); reviewLog = emptyMap(); profile = StudentProfile()
        prefs.edit().clear().apply()
        context.getSharedPreferences("avenor_data", Context.MODE_PRIVATE).edit().clear().apply()
        saveProfile()
        tab = Destination.TODAY
    }

    // ---------- Persistence ----------

    private fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    private fun saveTasks() = put("tasks", JSONArray(tasks.map { taskJson(it) }).toString())
    private fun saveClasses() = put("classes", JSONArray(classes.map { classJson(it) }).toString())
    private fun saveEvents() = put("events", JSONArray(events.map { eventJson(it) }).toString())
    private fun saveSessions() = put("sessions", JSONArray(sessions.map { sessionJson(it) }).toString())
    private fun saveCards() = put("cards", JSONArray(cards.map { cardJson(it) }).toString())
    private fun saveReviewLog() = put("reviewLog", JSONObject(reviewLog.mapKeys { it.key.toString() }).toString())
    private fun saveProfile() = put("profile", profileJson(profile).toString())

    private fun array(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    private fun load() {
        tasks = array("tasks").map { taskFrom(it) }
        classes = array("classes").map { classFrom(it) }
        events = array("events").mapNotNull { eventFrom(it) }
        sessions = array("sessions").map { sessionFrom(it) }
        cards = array("cards").map { cardFrom(it) }
        reviewLog = try {
            val o = JSONObject(prefs.getString("reviewLog", "{}") ?: "{}")
            o.keys().asSequence().associate { LocalDate.parse(it) to o.optInt(it) }
        } catch (_: Exception) { emptyMap() }
        profile = prefs.getString("profile", null)?.let { runCatching { profileFrom(JSONObject(it)) }.getOrNull() } ?: StudentProfile()
    }

    /** Version 01 stored free-text dates in "avenor_data". Bring that data forward so nobody loses work. */
    private fun migrateFromVersion01() {
        val old = context.getSharedPreferences("avenor_data", Context.MODE_PRIVATE)
        fun oldArray(key: String): List<JSONObject> = try {
            val a = JSONArray(old.getString(key, "[]") ?: "[]"); (0 until a.length()).mapNotNull { a.optJSONObject(it) }
        } catch (_: Exception) { emptyList() }

        val today = LocalDate.now()
        val migratedClasses = oldArray("classes").mapIndexed { i, o ->
            AvenorClass(id = o.opt("id")?.toString() ?: newId(), name = o.optString("name", "Class"),
                teacher = o.optString("teacher"), room = o.optString("room"), colorIndex = i)
        }
        val migratedTasks = oldArray("tasks").map { o ->
            val rawDue = o.optString("dueDate")
            val due = parseLooseDate(rawDue, today)
            val className = o.optString("className")
            AvenorTask(
                id = o.opt("id")?.toString() ?: newId(),
                title = o.optString("title", "Untitled task"),
                details = listOf(o.optString("description"), if (due == null && rawDue.isNotBlank()) "Due: $rawDue" else "")
                    .filter { it.isNotBlank() }.joinToString("\n"),
                className = className,
                classId = migratedClasses.firstOrNull { it.name.equals(className, true) }?.id,
                dueDate = due,
                priority = Priority.from(o.optString("priority")),
                estimatedMinutes = o.optInt("estimatedMinutes", 30),
                completed = o.optBoolean("completed"),
            )
        }
        val migratedEvents = oldArray("events").map { o ->
            val day = parseLooseDate(o.optString("date"), today) ?: today
            AvenorEvent(
                id = o.opt("id")?.toString() ?: newId(), title = o.optString("title", "Event"),
                start = day.atStartOfDay(), hasTime = false,
                legacyTime = listOf(o.optString("startTime"), o.optString("endTime")).filter { it.isNotBlank() }.joinToString(" – "),
            )
        }
        classes = migratedClasses; tasks = migratedTasks; events = migratedEvents
        profile = StudentProfile(onboarded = migratedTasks.isNotEmpty() || migratedClasses.isNotEmpty() || migratedEvents.isNotEmpty())
        saveClasses(); saveTasks(); saveEvents(); saveProfile()
    }

    private fun parseLooseDate(raw: String, today: LocalDate): LocalDate? {
        if (raw.isBlank()) return null
        runCatching { return LocalDate.parse(raw.trim()) }
        val parsed = QuickAddParser.date(raw.trim().split(" ").first(), today)
        return parsed
    }

    // JSON mappers
    private fun taskJson(t: AvenorTask) = JSONObject().apply {
        put("id", t.id); put("title", t.title); put("details", t.details); put("classId", t.classId ?: JSONObject.NULL)
        put("className", t.className); put("dueDate", t.dueDate?.toString() ?: JSONObject.NULL); put("priority", t.priority.name)
        put("estimatedMinutes", t.estimatedMinutes); put("completed", t.completed); put("createdAt", t.createdAt)
        put("completedAt", t.completedAt ?: JSONObject.NULL); put("focusedMinutes", t.focusedMinutes)
    }
    private fun taskFrom(o: JSONObject) = AvenorTask(
        id = o.optString("id", newId()), title = o.optString("title", "Untitled task"), details = o.optString("details"),
        classId = o.optNullableString("classId"), className = o.optString("className"),
        dueDate = o.optNullableString("dueDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        priority = Priority.from(o.optString("priority")), estimatedMinutes = o.optInt("estimatedMinutes", 30),
        completed = o.optBoolean("completed"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        completedAt = if (o.isNull("completedAt") || !o.has("completedAt")) null else o.optLong("completedAt"),
        focusedMinutes = o.optInt("focusedMinutes"),
    )
    private fun classJson(c: AvenorClass) = JSONObject().apply {
        put("id", c.id); put("name", c.name); put("teacher", c.teacher); put("room", c.room); put("colorIndex", c.colorIndex)
    }
    private fun classFrom(o: JSONObject) = AvenorClass(o.optString("id", newId()), o.optString("name", "Class"),
        o.optString("teacher"), o.optString("room"), o.optInt("colorIndex"))
    private fun eventJson(e: AvenorEvent) = JSONObject().apply {
        put("id", e.id); put("title", e.title); put("start", e.start.toString()); put("hasTime", e.hasTime)
        put("durationMinutes", e.durationMinutes); put("classId", e.classId ?: JSONObject.NULL); put("legacyTime", e.legacyTime)
    }
    private fun eventFrom(o: JSONObject): AvenorEvent? {
        val start = runCatching { LocalDateTime.parse(o.optString("start")) }.getOrNull() ?: return null
        return AvenorEvent(o.optString("id", newId()), o.optString("title", "Event"), start, o.optBoolean("hasTime", true),
            o.optInt("durationMinutes", 60), o.optNullableString("classId"), o.optString("legacyTime"))
    }
    private fun sessionJson(s: FocusSession) = JSONObject().apply {
        put("id", s.id); put("taskId", s.taskId ?: JSONObject.NULL); put("label", s.label); put("plannedMinutes", s.plannedMinutes)
        put("focusedSeconds", s.focusedSeconds); put("startedAt", s.startedAt); put("endedAt", s.endedAt); put("reachedGoal", s.reachedGoal)
    }
    private fun sessionFrom(o: JSONObject) = FocusSession(o.optString("id", newId()), o.optNullableString("taskId"),
        o.optString("label", "Focus session"), o.optInt("plannedMinutes"), o.optInt("focusedSeconds"),
        o.optLong("startedAt"), o.optLong("endedAt"), o.optBoolean("reachedGoal"))
    private fun cardJson(c: Flashcard) = JSONObject().apply {
        put("id", c.id); put("classId", c.classId ?: JSONObject.NULL); put("front", c.front); put("back", c.back); put("box", c.box)
        put("dueAt", c.dueAt); put("createdAt", c.createdAt); put("lastReviewed", c.lastReviewed ?: JSONObject.NULL); put("lapses", c.lapses)
    }
    private fun cardFrom(o: JSONObject) = Flashcard(o.optString("id", newId()), o.optNullableString("classId"),
        o.optString("front"), o.optString("back"), o.optInt("box", 1), o.optLong("dueAt", System.currentTimeMillis()),
        o.optLong("createdAt", System.currentTimeMillis()),
        if (o.isNull("lastReviewed") || !o.has("lastReviewed")) null else o.optLong("lastReviewed"), o.optInt("lapses"))
    private fun profileJson(p: StudentProfile) = JSONObject().apply {
        put("name", p.name); put("grade", p.grade.name); put("dailyGoalMinutes", p.dailyGoalMinutes)
        put("defaultSessionMinutes", p.defaultSessionMinutes); put("onboarded", p.onboarded)
        put("remindersEnabled", p.remindersEnabled); put("planReminderHour", p.planReminderHour); put("nudgeWhenLeaving", p.nudgeWhenLeaving)
    }
    private fun profileFrom(o: JSONObject) = StudentProfile(o.optString("name"), GradeLevel.from(o.optString("grade")),
        o.optInt("dailyGoalMinutes", 60), o.optInt("defaultSessionMinutes", 25), o.optBoolean("onboarded"),
        o.optBoolean("remindersEnabled"), o.optInt("planReminderHour", 16), o.optBoolean("nudgeWhenLeaving", true))
}

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

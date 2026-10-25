package com.example.avenor.domain

import com.example.avenor.data.AvenorClass
import com.example.avenor.data.AvenorTask
import com.example.avenor.data.Flashcard
import com.example.avenor.data.Priority
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

// Pure logic shared in spirit with iOS Engine.swift. Tesler's Law: the app absorbs the complexity
// (what to do next, parsing, scheduling) so the student doesn't have to.

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

enum class TaskBucket(val label: String) {
    OVERDUE("Overdue"), TODAY("Today"), TOMORROW("Tomorrow"), WEEK("This week"), LATER("Later"), SOMEDAY("No due date")
}

object Planner {
    private fun daysUntil(due: LocalDate, today: LocalDate) = ChronoUnit.DAYS.between(today, due).toInt()

    /** Urgency × importance, with a small boost for quick wins. */
    fun score(task: AvenorTask, today: LocalDate = LocalDate.now()): Double {
        val urgency = task.dueDate?.let {
            when (daysUntil(it, today)) {
                in Int.MIN_VALUE..-1 -> 6.0
                0 -> 5.0
                1 -> 4.0
                in 2..3 -> 3.0
                in 4..7 -> 2.0
                else -> 1.0
            }
        } ?: 0.75
        val remaining = (task.estimatedMinutes - task.focusedMinutes).coerceAtLeast(0)
        val quickWin = if (remaining <= 15) 0.5 else 0.0
        return urgency * 2 + task.priority.weight * 1.5 + quickWin
    }

    fun ranked(tasks: List<AvenorTask>, today: LocalDate = LocalDate.now()): List<AvenorTask> =
        tasks.filter { !it.completed }
            .sortedWith(compareByDescending<AvenorTask> { score(it, today) }.thenBy { it.createdAt })

    fun bucket(task: AvenorTask, today: LocalDate = LocalDate.now()): TaskBucket {
        val due = task.dueDate ?: return TaskBucket.SOMEDAY
        return when (daysUntil(due, today)) {
            in Int.MIN_VALUE..-1 -> TaskBucket.OVERDUE
            0 -> TaskBucket.TODAY
            1 -> TaskBucket.TOMORROW
            in 2..6 -> TaskBucket.WEEK
            else -> TaskBucket.LATER
        }
    }

    /** Miller's Law: big work is shown as focus-sized chunks. */
    fun sessions(minutes: Int, sessionLength: Int): Int {
        val length = sessionLength.coerceAtLeast(5)
        return ceil(minutes.coerceAtLeast(1).toDouble() / length).toInt().coerceAtLeast(1)
    }

    private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun dueLabel(due: LocalDate, today: LocalDate = LocalDate.now()): String = when (daysUntil(due, today)) {
        in Int.MIN_VALUE..-1 -> "Overdue · ${due.format(shortDate)}"
        0 -> "Today"
        1 -> "Tomorrow"
        in 2..6 -> due.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        else -> due.format(shortDate)
    }

    fun reason(task: AvenorTask, sessionLength: Int, today: LocalDate = LocalDate.now()): String {
        val parts = mutableListOf<String>()
        task.dueDate?.let {
            parts += when (daysUntil(it, today)) {
                in Int.MIN_VALUE..-1 -> "Overdue"
                0 -> "Due today"
                1 -> "Due tomorrow"
                else -> "Due ${dueLabel(it, today)}"
            }
        }
        if (task.priority == Priority.HIGH) parts += "High priority"
        val remaining = (task.estimatedMinutes - task.focusedMinutes).coerceAtLeast(5)
        val count = sessions(remaining, sessionLength)
        parts += if (count == 1) "1 session" else "$count sessions"
        return parts.joinToString(" · ")
    }
}

// ---------- Quick add: "Bio essay fri 45m !" ----------

data class QuickAddResult(
    val title: String = "",
    val dueDate: LocalDate? = null,
    val minutes: Int? = null,
    val priority: Priority? = null,
    val classId: String? = null,
    val className: String? = null,
)

object QuickAddParser {
    private val fullDays = mapOf(
        "sunday" to DayOfWeek.SUNDAY, "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY, "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
    )
    private val shortDays = mapOf(
        "sun" to DayOfWeek.SUNDAY, "mon" to DayOfWeek.MONDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY, "weds" to DayOfWeek.WEDNESDAY, "thu" to DayOfWeek.THURSDAY,
        "thur" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY, "fri" to DayOfWeek.FRIDAY, "sat" to DayOfWeek.SATURDAY,
    )

    private fun clean(word: String) = word.lowercase().trim(',', '.', ';')

    fun parse(input: String, classes: List<AvenorClass>, today: LocalDate = LocalDate.now()): QuickAddResult {
        var result = QuickAddResult()
        val titleWords = mutableListOf<String>()
        val words = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        var i = 0
        while (i < words.size) {
            val raw = words[i]
            val word = clean(raw)
            val minutes = duration(word)
            val tagged = if (word.startsWith("#") && word.length > 1) matchClass(word.drop(1), classes) else null
            val nextDate = if (word in setOf("due", "by", "on") && i + 1 < words.size) date(words[i + 1], today) else null
            val thisDate = date(raw, today)
            when {
                word in setOf("!", "!!", "!!!", "!high", "!h") -> result = result.copy(priority = Priority.HIGH)
                word in setOf("!low", "!l") -> result = result.copy(priority = Priority.LOW)
                word in setOf("!med", "!medium", "!m") -> result = result.copy(priority = Priority.MEDIUM)
                minutes != null -> result = result.copy(minutes = minutes)
                tagged != null -> result = result.copy(classId = tagged.id, className = tagged.name)
                nextDate != null -> { result = result.copy(dueDate = nextDate); i += 1 }
                thisDate != null -> result = result.copy(dueDate = thisDate)
                else -> titleWords += raw
            }
            i += 1
        }
        val title = titleWords.joinToString(" ")
        result = result.copy(title = title)
        if (result.classId == null) {
            val lower = title.lowercase()
            val tokens = lower.split(Regex("[^\\p{L}\\p{N}]+")).toSet()
            classes.firstOrNull { c ->
                val name = c.name.lowercase()
                name in tokens || (name.length >= 4 && lower.contains(name)) ||
                    tokens.any { it.length >= 3 && name.startsWith(it) }   // "bio" → Biology
            }?.let { result = result.copy(classId = it.id, className = it.name) }
        }
        return result
    }

    fun duration(word: String): Int? {
        val units = listOf("mins" to 1.0, "min" to 1.0, "m" to 1.0, "hrs" to 60.0, "hr" to 60.0, "h" to 60.0)
        for ((suffix, multiplier) in units) {
            if (word.endsWith(suffix)) {
                val value = word.dropLast(suffix.length).toDoubleOrNull() ?: continue
                val total = value * multiplier
                if (value > 0 && total <= 600) return total.roundToInt()
            }
        }
        return null
    }

    fun date(rawWord: String, today: LocalDate): LocalDate? {
        val word = clean(rawWord)
        when (word) {
            "today", "tonight" -> return today
            "tomorrow", "tmrw", "tmr" -> return today.plusDays(1)
        }
        // Abbreviations count only when typed lowercase, so "SAT prep" stays a title, not Saturday.
        val allCaps = rawWord.length > 1 && rawWord == rawWord.uppercase()
        val day = fullDays[word] ?: if (allCaps) null else shortDays[word]
        if (day != null) {
            val delta = (day.value - today.dayOfWeek.value + 7) % 7
            return today.plusDays(delta.toLong())
        }
        val pieces = word.split("/")
        val numbers = pieces.mapNotNull { it.toIntOrNull() }
        if (pieces.size !in 2..3 || numbers.size != pieces.size) return null
        val month = numbers[0]
        val dayOfMonth = numbers[1]
        if (month !in 1..12 || dayOfMonth !in 1..31) return null
        return try {
            if (numbers.size == 3) {
                val year = if (numbers[2] < 100) 2000 + numbers[2] else numbers[2]
                LocalDate.of(year, month, dayOfMonth)
            } else {
                val candidate = LocalDate.of(today.year, month, dayOfMonth)
                if (candidate.isBefore(today)) candidate.plusYears(1) else candidate
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun matchClass(tag: String, classes: List<AvenorClass>): AvenorClass? = classes.firstOrNull {
        val name = it.name.lowercase()
        name.startsWith(tag) || name.replace(" ", "").startsWith(tag)
    }
}

// ---------- Spaced repetition ----------

object Leitner {
    private val intervals = mapOf(1 to 1L, 2 to 2L, 3 to 4L, 4 to 8L, 5 to 16L)

    fun review(card: Flashcard, remembered: Boolean, relearning: Boolean, now: Long = System.currentTimeMillis()): Flashcard {
        return if (remembered) {
            val box = if (relearning) 1 else (card.box + 1).coerceAtMost(5)
            val due = now.toLocalDate().plusDays(intervals[box] ?: 1L).startMillis()
            card.copy(box = box, dueAt = due, lastReviewed = now)
        } else {
            card.copy(box = 1, lapses = card.lapses + 1, dueAt = now + 10 * 60 * 1000, lastReviewed = now)
        }
    }
}

// ---------- Kind streaks ----------

object Streaks {
    /** Consecutive active days ending today (or yesterday). One missed day is forgiven per week of streak. */
    fun current(activeDays: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        var day = if (today in activeDays) today else today.minusDays(1)
        var count = 0
        var gracesUsed = 0
        repeat(3650) {
            if (day in activeDays) {
                count += 1
            } else {
                val allowed = 1 + count / 7
                if (count > 0 && gracesUsed < allowed && day.minusDays(1) in activeDays) {
                    gracesUsed += 1
                } else {
                    return count
                }
            }
            day = day.minusDays(1)
        }
        return count
    }
}

package com.example.avenor.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Same records as the iOS app (Models.swift) so features and wording stay in sync across platforms.

enum class Priority(val label: String, val weight: Double) {
    LOW("Low", 1.0),
    MEDIUM("Medium", 2.0),
    HIGH("High", 3.0);

    companion object {
        fun from(raw: String?): Priority =
            entries.firstOrNull { it.name.equals(raw, true) || it.label.equals(raw, true) } ?: MEDIUM
    }
}

enum class GradeLevel(val label: String, val suggestedDailyGoal: Int) {
    G8("8th grade", 45),
    G9("9th grade", 60),
    G10("10th grade", 60),
    G11("11th grade", 90),
    G12("12th grade", 90),
    COLLEGE("College", 120);

    companion object {
        fun from(raw: String?): GradeLevel = entries.firstOrNull { it.name == raw } ?: G9
    }
}

fun newId(): String = UUID.randomUUID().toString()

data class AvenorTask(
    val id: String = newId(),
    val title: String,
    val details: String = "",
    val classId: String? = null,
    val className: String = "",          // legacy free-text class from version 01
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val estimatedMinutes: Int = 30,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val focusedMinutes: Int = 0,
)

data class AvenorClass(
    val id: String = newId(),
    val name: String,
    val teacher: String = "",
    val room: String = "",
    val colorIndex: Int = 0,
)

data class AvenorEvent(
    val id: String = newId(),
    val title: String,
    val start: LocalDateTime,
    val hasTime: Boolean = true,
    val durationMinutes: Int = 60,
    val classId: String? = null,
    val legacyTime: String = "",          // free-text "3:00 PM – 4:00 PM" from version 01
)

data class FocusSession(
    val id: String = newId(),
    val taskId: String? = null,
    val label: String,
    val plannedMinutes: Int,
    val focusedSeconds: Int,
    val startedAt: Long,
    val endedAt: Long,
    val reachedGoal: Boolean,
) {
    val focusedMinutes: Int get() = focusedSeconds / 60
}

/** Spaced-repetition card scheduled with 5 Leitner boxes (1, 2, 4, 8, 16 days). */
data class Flashcard(
    val id: String = newId(),
    val classId: String? = null,
    val front: String,
    val back: String,
    val box: Int = 1,
    val dueAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewed: Long? = null,
    val lapses: Int = 0,
)

data class StudentProfile(
    val name: String = "",
    val grade: GradeLevel = GradeLevel.G9,
    val dailyGoalMinutes: Int = 60,
    val defaultSessionMinutes: Int = 25,
    val onboarded: Boolean = false,
    val remindersEnabled: Boolean = false,
    val planReminderHour: Int = 16,
    val nudgeWhenLeaving: Boolean = true,
)

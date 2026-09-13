package com.example.avenor.model

data class AvenorTask(
    val id: Long,
    val title: String,
    val description: String,
    val className: String,
    val dueDate: String,
    val priority: String,
    val estimatedMinutes: Int,
    val completed: Boolean
)

data class AvenorClass(
    val id: Long,
    val name: String,
    val teacher: String,
    val room: String
)

data class AvenorEvent(
    val id: Long,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String
)

enum class AvenorPage {
    HOME,
    TASKS,
    CALENDAR,
    CLASSES,
    PROFILE
}

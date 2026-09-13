package com.example.avenor.data

import android.content.Context
import com.example.avenor.model.AvenorClass
import com.example.avenor.model.AvenorEvent
import com.example.avenor.model.AvenorTask
import org.json.JSONArray
import org.json.JSONObject

class AvenorStorage(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveTasks(tasks: List<AvenorTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("className", task.className)
                    put("dueDate", task.dueDate)
                    put("priority", task.priority)
                    put("estimatedMinutes", task.estimatedMinutes)
                    put("completed", task.completed)
                }
            )
        }
        preferences.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    fun loadTasks(): List<AvenorTask> =
        loadArray(KEY_TASKS).mapNotNull { item ->
            runCatching {
                AvenorTask(
                    id = item.getLong("id"),
                    title = item.getString("title"),
                    description = item.optString("description"),
                    className = item.optString("className"),
                    dueDate = item.optString("dueDate"),
                    priority = item.optString("priority", "Medium"),
                    estimatedMinutes = item.optInt("estimatedMinutes", 30).coerceAtLeast(1),
                    completed = item.optBoolean("completed", false)
                )
            }.getOrNull()
        }

    fun saveClasses(classes: List<AvenorClass>) {
        val array = JSONArray()
        classes.forEach { schoolClass ->
            array.put(
                JSONObject().apply {
                    put("id", schoolClass.id)
                    put("name", schoolClass.name)
                    put("teacher", schoolClass.teacher)
                    put("room", schoolClass.room)
                }
            )
        }
        preferences.edit().putString(KEY_CLASSES, array.toString()).apply()
    }

    fun loadClasses(): List<AvenorClass> =
        loadArray(KEY_CLASSES).mapNotNull { item ->
            runCatching {
                AvenorClass(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    teacher = item.optString("teacher"),
                    room = item.optString("room")
                )
            }.getOrNull()
        }

    fun saveEvents(events: List<AvenorEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put("id", event.id)
                    put("title", event.title)
                    put("date", event.date)
                    put("startTime", event.startTime)
                    put("endTime", event.endTime)
                }
            )
        }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    fun loadEvents(): List<AvenorEvent> =
        loadArray(KEY_EVENTS).mapNotNull { item ->
            runCatching {
                AvenorEvent(
                    id = item.getLong("id"),
                    title = item.getString("title"),
                    date = item.optString("date"),
                    startTime = item.optString("startTime"),
                    endTime = item.optString("endTime")
                )
            }.getOrNull()
        }

    private fun loadArray(key: String): List<JSONObject> {
        val raw = preferences.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index))
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES_NAME = "avenor_data"
        const val KEY_TASKS = "tasks"
        const val KEY_CLASSES = "classes"
        const val KEY_EVENTS = "events"
    }
}

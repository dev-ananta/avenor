package com.example.avenor.domain

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.avenor.data.FocusSession
import com.example.avenor.notify.Reminders

enum class FocusPhase { IDLE, FOCUSING, PAUSED, FINISHED, ON_BREAK }

/**
 * One study round: focus → wrap-up (recall) → break → next round. Mirrors iOS FocusEngine.swift.
 * Time comes from timestamps, so it stays correct when the screen is off or the app is in the background.
 */
class FocusEngine(private val context: Context) {
    var phase by mutableStateOf(FocusPhase.IDLE)
        private set
    var taskId by mutableStateOf<String?>(null)
        private set
    var label by mutableStateOf("")
        private set
    var plannedMillis by mutableStateOf(25L * 60_000)
        private set
    var breakEndsAt by mutableStateOf<Long?>(null)
        private set
    var lastSession by mutableStateOf<FocusSession?>(null)
        private set
    var roundsCompleted by mutableIntStateOf(0)
        private set

    private var segmentStart: Long? = null
    private var banked = 0L
    private var startedAt = 0L

    val isActive: Boolean get() = phase != FocusPhase.IDLE

    fun elapsed(now: Long = System.currentTimeMillis()): Long = banked + (segmentStart?.let { now - it } ?: 0L)
    fun remaining(now: Long = System.currentTimeMillis()): Long = (plannedMillis - elapsed(now)).coerceAtLeast(0)
    fun progress(now: Long = System.currentTimeMillis()): Float =
        if (plannedMillis > 0) (elapsed(now).toFloat() / plannedMillis).coerceIn(0f, 1f) else 0f

    fun start(taskId: String?, label: String, minutes: Int) {
        this.taskId = taskId
        this.label = label.trim().ifEmpty { "Focus session" }
        plannedMillis = minutes.coerceAtLeast(1) * 60_000L
        banked = 0
        startedAt = System.currentTimeMillis()
        segmentStart = startedAt
        breakEndsAt = null
        lastSession = null
        phase = FocusPhase.FOCUSING
        Reminders.cancelFocus(context)
        Reminders.scheduleFocusEnd(context, startedAt + plannedMillis, this.label)
    }

    fun pause() {
        if (phase != FocusPhase.FOCUSING) return
        banked = elapsed(); segmentStart = null
        phase = FocusPhase.PAUSED
        Reminders.cancelFocus(context)
    }

    fun resume() {
        if (phase != FocusPhase.PAUSED) return
        segmentStart = System.currentTimeMillis()
        phase = FocusPhase.FOCUSING
        Reminders.scheduleFocusEnd(context, System.currentTimeMillis() + remaining(), label)
    }

    /** Ends the block (on time or early). Returns the session to log, or null if it was under a minute. */
    fun complete(): FocusSession? {
        if (phase != FocusPhase.FOCUSING && phase != FocusPhase.PAUSED) return null
        val millis = elapsed().coerceAtMost(plannedMillis)
        banked = millis; segmentStart = null
        phase = FocusPhase.FINISHED
        Reminders.cancelFocus(context)
        val session = FocusSession(
            taskId = taskId, label = label, plannedMinutes = (plannedMillis / 60_000).toInt(),
            focusedSeconds = (millis / 1000).toInt(), startedAt = startedAt, endedAt = System.currentTimeMillis(),
            reachedGoal = millis >= plannedMillis - 1000,
        )
        if (session.reachedGoal) roundsCompleted += 1
        lastSession = session
        return if (millis >= 60_000) session else null
    }

    /** Pomodoro rhythm: every 4th completed round earns a longer break. */
    val suggestedBreakMinutes: Int get() = if (roundsCompleted > 0 && roundsCompleted % 4 == 0) 15 else 5

    fun startBreak(minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        breakEndsAt = end
        phase = FocusPhase.ON_BREAK
        Reminders.scheduleBreakEnd(context, end)
    }

    fun nextRound(minutes: Int) = start(taskId, label, minutes)

    fun close() {
        phase = FocusPhase.IDLE
        segmentStart = null; banked = 0; breakEndsAt = null; roundsCompleted = 0
        Reminders.cancelFocus(context)
    }

    fun onAppBackgrounded(nudge: Boolean) {
        if (phase == FocusPhase.FOCUSING && nudge) Reminders.scheduleComeBack(context, remaining())
    }

    fun onAppForegrounded() = Reminders.cancelComeBack(context)
}

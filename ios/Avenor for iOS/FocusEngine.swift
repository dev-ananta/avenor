import Foundation
import Combine

/// Drives one study round: focus → wrap-up (recall) → break → next round.
/// Time is computed from timestamps, not a ticking counter, so it stays correct
/// when the phone is locked or the app is in the background.
@MainActor
final class FocusEngine: ObservableObject {
    enum Phase: Equatable {
        case idle, focusing, paused, finished, onBreak
    }

    @Published private(set) var phase: Phase = .idle
    @Published var isPresented = false
    @Published private(set) var taskID: UUID?
    @Published private(set) var label = ""
    @Published private(set) var plannedSeconds: TimeInterval = 25 * 60
    @Published private(set) var breakEndsAt: Date?
    @Published private(set) var lastSession: FocusSession?
    @Published private(set) var roundsCompleted = 0

    private var segmentStart: Date?
    private var banked: TimeInterval = 0
    private var startedAt = Date()

    func elapsed(at now: Date = Date()) -> TimeInterval {
        banked + (segmentStart.map { now.timeIntervalSince($0) } ?? 0)
    }

    func remaining(at now: Date = Date()) -> TimeInterval {
        max(0, plannedSeconds - elapsed(at: now))
    }

    func progress(at now: Date = Date()) -> Double {
        plannedSeconds > 0 ? min(1, elapsed(at: now) / plannedSeconds) : 0
    }

    func start(taskID: UUID?, label: String, minutes: Int) {
        let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
        self.taskID = taskID
        self.label = trimmed.isEmpty ? "Focus session" : trimmed
        plannedSeconds = TimeInterval(max(1, minutes) * 60)
        banked = 0
        startedAt = Date()
        segmentStart = startedAt
        breakEndsAt = nil
        lastSession = nil
        phase = .focusing
        isPresented = true
        Reminders.cancelFocusNotifications()
        Reminders.scheduleFocusEnd(in: plannedSeconds, label: self.label)
    }

    func pause() {
        guard phase == .focusing else { return }
        banked = elapsed()
        segmentStart = nil
        phase = .paused
        Reminders.cancelFocusNotifications()
    }

    func resume() {
        guard phase == .paused else { return }
        segmentStart = Date()
        phase = .focusing
        Reminders.scheduleFocusEnd(in: remaining(), label: label)
    }

    /// Ends the focus block (on time or early). Returns the session to log, or nil if it was under a minute.
    func complete() -> FocusSession? {
        guard phase == .focusing || phase == .paused else { return nil }
        let seconds = Int(min(elapsed(), plannedSeconds))
        banked = TimeInterval(seconds)
        segmentStart = nil
        phase = .finished
        Reminders.cancelFocusNotifications()
        let session = FocusSession(
            taskID: taskID,
            label: label,
            plannedMinutes: Int(plannedSeconds / 60),
            focusedSeconds: seconds,
            startedAt: startedAt,
            endedAt: Date(),
            reachedGoal: Double(seconds) >= plannedSeconds - 1
        )
        if session.reachedGoal { roundsCompleted += 1 }
        lastSession = session
        return seconds >= 60 ? session : nil
    }

    /// Classic Pomodoro rhythm: every 4th completed round earns a longer break.
    var suggestedBreakMinutes: Int {
        roundsCompleted > 0 && roundsCompleted % 4 == 0 ? 15 : 5
    }

    func startBreak(minutes: Int) {
        let seconds = TimeInterval(minutes * 60)
        breakEndsAt = Date().addingTimeInterval(seconds)
        phase = .onBreak
        Reminders.scheduleBreakEnd(in: seconds)
    }

    func nextRound(minutes: Int) {
        start(taskID: taskID, label: label, minutes: minutes)
    }

    func close() {
        phase = .idle
        isPresented = false
        segmentStart = nil
        banked = 0
        breakEndsAt = nil
        roundsCompleted = 0
        Reminders.cancelFocusNotifications()
    }

    func sceneChanged(toBackground: Bool, nudge: Bool) {
        if toBackground {
            if phase == .focusing && nudge { Reminders.scheduleComeBack(remaining: remaining()) }
        } else {
            Reminders.cancelComeBack()
        }
    }
}

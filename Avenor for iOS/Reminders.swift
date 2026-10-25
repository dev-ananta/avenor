import Foundation
import UserNotifications

/// Local notifications only — scheduled on the phone itself, so there is no push server to pay for.
enum Reminders {
    private static var center: UNUserNotificationCenter { UNUserNotificationCenter.current() }

    private static let focusEndID = "focus-end"
    private static let breakEndID = "break-end"
    private static let comeBackID = "focus-come-back"
    private static let dailyPlanID = "daily-plan"

    static func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
    }

    // MARK: Tasks — sensible defaults instead of a settings maze (Tesler's Law)

    private static func taskIDs(_ id: UUID) -> [String] {
        ["task-\(id.uuidString)-eve", "task-\(id.uuidString)-day"]
    }

    static func cancelTask(id: UUID) {
        center.removePendingNotificationRequests(withIdentifiers: taskIDs(id))
    }

    static func sync(task: AvenorTask, enabled: Bool) {
        cancelTask(id: task.id)
        guard enabled, !task.completed, let due = task.dueDate else { return }
        let calendar = Calendar.current
        let dueDay = calendar.startOfDay(for: due)
        let ids = taskIDs(task.id)
        let now = Date()
        // 7 PM the night before…
        if let evening = calendar.date(byAdding: .hour, value: -5, to: dueDay), evening > now {
            schedule(id: ids[0],
                     title: "Due tomorrow: \(task.title)",
                     body: "One focus session tonight makes tomorrow easy. It's already queued in Avenor.",
                     at: evening)
        }
        // …and 7 AM the day of.
        if let morning = calendar.date(byAdding: .hour, value: 7, to: dueDay), morning > now {
            schedule(id: ids[1],
                     title: "Due today: \(task.title)",
                     body: "Open Avenor and tap Start — it's at the top of your list.",
                     at: morning)
        }
    }

    static func resyncAll(tasks: [AvenorTask], profile: StudentProfile) {
        for task in tasks { sync(task: task, enabled: profile.remindersEnabled) }
        center.removePendingNotificationRequests(withIdentifiers: [dailyPlanID])
        guard profile.remindersEnabled else { return }
        let content = UNMutableNotificationContent()
        content.title = "Two-minute plan"
        content.body = "Check what's next and start one session. Small starts beat big plans."
        content.sound = .default
        var components = DateComponents()
        components.hour = profile.planReminderHour
        components.minute = 0
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        center.add(UNNotificationRequest(identifier: dailyPlanID, content: content, trigger: trigger), withCompletionHandler: nil)
    }

    // MARK: Focus sessions

    static func scheduleFocusEnd(in seconds: TimeInterval, label: String) {
        scheduleAfter(seconds, id: focusEndID, title: "Session complete 🎉", body: "Nice work on \(label). Come back to lock in what you learned.")
    }

    static func scheduleBreakEnd(in seconds: TimeInterval) {
        scheduleAfter(seconds, id: breakEndID, title: "Break's over", body: "Ready for another round? Your next session is one tap away.")
    }

    /// Fires only if the student is still away 90 seconds after leaving mid-session.
    static func scheduleComeBack(remaining: TimeInterval) {
        let minutes = max(1, Int(remaining / 60))
        scheduleAfter(90, id: comeBackID,
                      title: "Your focus session is still running",
                      body: "\(minutes) min left. If you locked your phone to focus, ignore this. Otherwise — come back and finish strong.")
    }

    static func cancelComeBack() {
        center.removePendingNotificationRequests(withIdentifiers: [comeBackID])
    }

    static func cancelFocusNotifications() {
        center.removePendingNotificationRequests(withIdentifiers: [focusEndID, breakEndID, comeBackID])
    }

    static func cancelAll() {
        center.removeAllPendingNotificationRequests()
    }

    // MARK: Helpers

    private static func schedule(id: String, title: String, body: String, at date: Date) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger), withCompletionHandler: nil)
    }

    private static func scheduleAfter(_ seconds: TimeInterval, id: String, title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, seconds), repeats: false)
        center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger), withCompletionHandler: nil)
    }
}

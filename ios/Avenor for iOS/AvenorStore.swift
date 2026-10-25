import Foundation
import Combine

/// Single source of truth. Everything is stored on-device (UserDefaults + Codable):
/// no servers, no accounts, no analytics — which is also why running Avenor costs almost nothing.
@MainActor
final class AvenorStore: ObservableObject {
    @Published var tasks: [AvenorTask] = [] { didSet { save(tasks, key: "tasks") } }
    @Published var classes: [AvenorClass] = [] { didSet { save(classes, key: "classes") } }
    @Published var events: [AvenorEvent] = [] { didSet { save(events, key: "events") } }
    @Published var sessions: [FocusSession] = [] { didSet { save(sessions, key: "sessions") } }
    @Published var cards: [Flashcard] = [] { didSet { save(cards, key: "cards") } }
    @Published var reviewLog: [Date: Int] = [:] { didSet { save(reviewLog, key: "reviewLog") } }
    @Published var profile = StudentProfile() { didSet { save(profile, key: "profile") } }

    // Navigation + transient UI state (not persisted)
    @Published var tab: Destination = .today
    @Published var recentlyDeleted: AvenorTask?

    private let calendar = Calendar.current

    init() {
        tasks = load([AvenorTask].self, key: "tasks") ?? []
        classes = load([AvenorClass].self, key: "classes") ?? []
        events = load([AvenorEvent].self, key: "events") ?? []
        sessions = load([FocusSession].self, key: "sessions") ?? []
        cards = load([Flashcard].self, key: "cards") ?? []
        reviewLog = load([Date: Int].self, key: "reviewLog") ?? [:]
        var loadedProfile = load(StudentProfile.self, key: "profile") ?? StudentProfile()
        // People upgrading from Avenor 1.0 already have data — don't make them sit through onboarding.
        if !loadedProfile.onboarded && (!tasks.isEmpty || !classes.isEmpty || !events.isEmpty) {
            loadedProfile.onboarded = true
        }
        profile = loadedProfile
    }

    // MARK: Tasks

    var rankedActive: [AvenorTask] { Planner.ranked(tasks) }

    func task(id: UUID?) -> AvenorTask? {
        guard let id else { return nil }
        return tasks.first { $0.id == id }
    }

    func addTask(_ task: AvenorTask) {
        tasks.insert(task, at: 0)
        Reminders.sync(task: task, enabled: profile.remindersEnabled)
    }

    func updateTask(_ task: AvenorTask) {
        guard let i = tasks.firstIndex(where: { $0.id == task.id }) else { return }
        tasks[i] = task
        Reminders.sync(task: task, enabled: profile.remindersEnabled)
    }

    func toggle(_ task: AvenorTask) {
        guard let i = tasks.firstIndex(where: { $0.id == task.id }) else { return }
        tasks[i].completed.toggle()
        tasks[i].completedAt = tasks[i].completed ? Date() : nil
        Reminders.sync(task: tasks[i], enabled: profile.remindersEnabled)
    }

    /// Deletes immediately but keeps a copy for the Undo toast (safer and faster than an "Are you sure?" alert).
    func deleteTask(id: UUID) {
        recentlyDeleted = tasks.first { $0.id == id }
        tasks.removeAll { $0.id == id }
        Reminders.cancelTask(id: id)
    }

    func undoDelete() {
        guard let task = recentlyDeleted else { return }
        tasks.insert(task, at: 0)
        recentlyDeleted = nil
        Reminders.sync(task: task, enabled: profile.remindersEnabled)
    }

    func className(for task: AvenorTask) -> String {
        if let cls = classInfo(id: task.classID) { return cls.name }
        return task.className
    }

    func tasksDue(on day: Date) -> [AvenorTask] {
        tasks.filter { task in
            guard let due = task.dueDate else { return false }
            return calendar.isDate(due, inSameDayAs: day)
        }
    }

    var tasksDoneToday: Int {
        tasks.filter { task in
            guard let done = task.completedAt else { return false }
            return calendar.isDateInToday(done)
        }.count
    }

    // MARK: Classes

    func classInfo(id: UUID?) -> AvenorClass? {
        guard let id else { return nil }
        return classes.first { $0.id == id }
    }

    func addClass(_ item: AvenorClass) { classes.append(item) }

    func updateClass(_ item: AvenorClass) {
        guard let i = classes.firstIndex(where: { $0.id == item.id }) else { return }
        classes[i] = item
        for t in tasks.indices where tasks[t].classID == item.id { tasks[t].className = item.name }
    }

    /// Removes the class but keeps its tasks and cards (they just become unfiled).
    func deleteClass(id: UUID) {
        classes.removeAll { $0.id == id }
        for t in tasks.indices where tasks[t].classID == id { tasks[t].classID = nil }
        for c in cards.indices where cards[c].classID == id { cards[c].classID = nil }
        for e in events.indices where events[e].classID == id { events[e].classID = nil }
    }

    // MARK: Events

    func addEvent(_ item: AvenorEvent) { events.append(item) }

    func updateEvent(_ item: AvenorEvent) {
        guard let i = events.firstIndex(where: { $0.id == item.id }) else { return }
        events[i] = item
    }

    func deleteEvent(id: UUID) { events.removeAll { $0.id == id } }

    func schedule(on day: Date) -> [AvenorEvent] {
        events.filter { calendar.isDate($0.date, inSameDayAs: day) }.sorted { $0.date < $1.date }
    }

    // MARK: Focus

    func log(_ session: FocusSession) {
        sessions.append(session)
        if let id = session.taskID, let i = tasks.firstIndex(where: { $0.id == id }) {
            tasks[i].focusedMinutes += session.focusedMinutes
        }
    }

    func focusMinutes(on day: Date) -> Int {
        sessions.filter { calendar.isDate($0.startedAt, inSameDayAs: day) }.reduce(0) { $0 + $1.focusedMinutes }
    }

    var todayFocusMinutes: Int { focusMinutes(on: Date()) }

    var dailyGoalProgress: Double {
        Double(todayFocusMinutes) / Double(max(profile.dailyGoalMinutes, 1))
    }

    var totalFocusMinutes: Int { sessions.reduce(0) { $0 + $1.focusedMinutes } }

    // MARK: Review

    var dueCards: [Flashcard] {
        let now = Date()
        return cards.filter { $0.dueAt <= now }.sorted { $0.dueAt < $1.dueAt }
    }

    var nextCardDue: Date? { cards.map(\.dueAt).filter { $0 > Date() }.min() }

    var reviewsToday: Int { reviewLog[calendar.startOfDay(for: Date())] ?? 0 }

    func addCard(_ card: Flashcard) { cards.append(card) }

    func updateCard(_ card: Flashcard) {
        guard let i = cards.firstIndex(where: { $0.id == card.id }) else { return }
        cards[i] = card
    }

    func deleteCard(id: UUID) { cards.removeAll { $0.id == id } }

    func review(cardID: UUID, remembered: Bool, relearning: Bool) {
        guard let i = cards.firstIndex(where: { $0.id == cardID }) else { return }
        cards[i] = Leitner.review(cards[i], remembered: remembered, relearning: relearning)
        reviewLog[calendar.startOfDay(for: Date()), default: 0] += 1
    }

    // MARK: Streak

    /// A day counts if you focused 5+ minutes, finished a task, or reviewed a card.
    var activeDays: Set<Date> {
        var days = Set<Date>()
        for s in sessions where s.focusedSeconds >= 300 { days.insert(calendar.startOfDay(for: s.startedAt)) }
        for t in tasks { if let done = t.completedAt { days.insert(calendar.startOfDay(for: done)) } }
        for (day, count) in reviewLog where count > 0 { days.insert(day) }
        return days
    }

    var streak: Int { Streaks.current(activeDays: activeDays) }

    // MARK: Reminders

    func setReminders(_ enabled: Bool) async {
        if enabled {
            let granted = await Reminders.requestAuthorization()
            profile.remindersEnabled = granted
        } else {
            profile.remindersEnabled = false
        }
        resyncReminders()
    }

    func resyncReminders() {
        Reminders.resyncAll(tasks: tasks, profile: profile)
    }

    // MARK: Reset

    func resetAll() {
        tasks = []; classes = []; events = []; sessions = []; cards = []; reviewLog = [:]
        profile = StudentProfile()
        Reminders.cancelAll()
        tab = .today
    }

    // MARK: Persistence

    private func save<T: Encodable>(_ value: T, key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        UserDefaults.standard.set(data, forKey: "avenor.\(key)")
    }

    private func load<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = UserDefaults.standard.data(forKey: "avenor.\(key)") else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
}

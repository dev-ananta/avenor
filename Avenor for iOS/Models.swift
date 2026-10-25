import Foundation

// MARK: - Enums

enum TaskPriority: String, Codable, CaseIterable, Identifiable {
    case low = "Low"
    case medium = "Medium"
    case high = "High"
    var id: String { rawValue }

    var weight: Double {
        switch self {
        case .low: return 1
        case .medium: return 2
        case .high: return 3
        }
    }
}

enum GradeLevel: String, Codable, CaseIterable, Identifiable {
    case g8 = "8th grade"
    case g9 = "9th grade"
    case g10 = "10th grade"
    case g11 = "11th grade"
    case g12 = "12th grade"
    case college = "College"
    var id: String { rawValue }

    /// Tesler's Law: the app picks a sensible starting goal so the student doesn't have to.
    var suggestedDailyGoal: Int {
        switch self {
        case .g8: return 45
        case .g9, .g10: return 60
        case .g11, .g12: return 90
        case .college: return 120
        }
    }
}

// MARK: - Core records
// Every record decodes with `decodeIfPresent` + defaults so data saved by Avenor 1.0
// keeps loading after new fields are added (no lost tasks after an update).

struct AvenorTask: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var title: String
    var details: String = ""
    var className: String = ""          // legacy free-text class; kept in sync with classID
    var classID: UUID? = nil
    var dueDate: Date? = nil
    var priority: TaskPriority = .medium
    var estimatedMinutes: Int = 30
    var completed: Bool = false
    var createdAt: Date = Date()
    var completedAt: Date? = nil
    var focusedMinutes: Int = 0
}

extension AvenorTask {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.value(.id, default: UUID())
        title = c.value(.title, default: "Untitled task")
        details = c.value(.details, default: "")
        className = c.value(.className, default: "")
        classID = c.optional(.classID)
        dueDate = c.optional(.dueDate)
        priority = c.value(.priority, default: .medium)
        estimatedMinutes = c.value(.estimatedMinutes, default: 30)
        completed = c.value(.completed, default: false)
        createdAt = c.value(.createdAt, default: Date())
        completedAt = c.optional(.completedAt)
        focusedMinutes = c.value(.focusedMinutes, default: 0)
    }
}

struct AvenorClass: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var name: String
    var teacher: String = ""
    var room: String = ""
    var colorIndex: Int = 0
}

extension AvenorClass {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.value(.id, default: UUID())
        name = c.value(.name, default: "Class")
        teacher = c.value(.teacher, default: "")
        room = c.value(.room, default: "")
        colorIndex = c.value(.colorIndex, default: name.unicodeScalars.reduce(0) { $0 + Int($1.value) } % 8)
    }
}

struct AvenorEvent: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var title: String
    /// Start of the event. When `hasTime` is false only the day matters.
    var date: Date = Date()
    var startTime: String = ""          // legacy free-text times from 1.0
    var endTime: String = ""
    var hasTime: Bool = false
    var durationMinutes: Int = 60
    var classID: UUID? = nil

    var timeLabel: String? {
        if hasTime {
            let end = date.addingTimeInterval(Double(durationMinutes) * 60)
            return "\(date.formatted(date: .omitted, time: .shortened)) – \(end.formatted(date: .omitted, time: .shortened))"
        }
        let legacy = [startTime, endTime].filter { !$0.isEmpty }.joined(separator: " – ")
        return legacy.isEmpty ? nil : legacy
    }
}

extension AvenorEvent {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.value(.id, default: UUID())
        title = c.value(.title, default: "Event")
        date = c.value(.date, default: Date())
        startTime = c.value(.startTime, default: "")
        endTime = c.value(.endTime, default: "")
        hasTime = c.value(.hasTime, default: false)
        durationMinutes = c.value(.durationMinutes, default: 60)
        classID = c.optional(.classID)
    }
}

// MARK: - Study records

struct FocusSession: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var taskID: UUID? = nil
    var label: String
    var plannedMinutes: Int
    var focusedSeconds: Int
    var startedAt: Date
    var endedAt: Date
    var reachedGoal: Bool

    var focusedMinutes: Int { focusedSeconds / 60 }
}

/// A spaced-repetition card scheduled with a 5-box Leitner system.
struct Flashcard: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var classID: UUID? = nil
    var front: String
    var back: String
    var box: Int = 1
    var dueAt: Date = Date()
    var createdAt: Date = Date()
    var lastReviewed: Date? = nil
    var lapses: Int = 0
}

struct StudentProfile: Codable, Equatable {
    var name: String = ""
    var grade: GradeLevel = .g9
    var dailyGoalMinutes: Int = 60
    var defaultSessionMinutes: Int = 25
    var onboarded: Bool = false
    var remindersEnabled: Bool = false
    var planReminderHour: Int = 16
    var nudgeWhenLeaving: Bool = true
}

extension StudentProfile {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        name = c.value(.name, default: "")
        grade = c.value(.grade, default: .g9)
        dailyGoalMinutes = c.value(.dailyGoalMinutes, default: 60)
        defaultSessionMinutes = c.value(.defaultSessionMinutes, default: 25)
        onboarded = c.value(.onboarded, default: false)
        remindersEnabled = c.value(.remindersEnabled, default: false)
        planReminderHour = c.value(.planReminderHour, default: 16)
        nudgeWhenLeaving = c.value(.nudgeWhenLeaving, default: true)
    }
}

// MARK: - Lenient decoding helpers

extension KeyedDecodingContainer {
    nonisolated func value<T: Decodable>(_ key: Key, default fallback: T) -> T {
        (try? decodeIfPresent(T.self, forKey: key)) ?? fallback
    }

    nonisolated func optional<T: Decodable>(_ key: Key) -> T? {
        try? decodeIfPresent(T.self, forKey: key)
    }
}

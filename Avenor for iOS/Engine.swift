import Foundation

// Pure, UI-free logic. Everything the app "decides for" the student lives here
// (Tesler's Law: the complexity has to live somewhere — put it in the app, not on the user).

// MARK: - Planner: what should I do next?

enum TaskBucket: String, CaseIterable, Identifiable, Hashable {
    case overdue = "Overdue"
    case today = "Today"
    case tomorrow = "Tomorrow"
    case week = "This week"
    case later = "Later"
    case someday = "No due date"
    var id: String { rawValue }
}

enum Planner {
    /// Urgency × importance score. Overdue and due-soon work floats up; quick wins get a small boost
    /// so a student who only has 15 minutes still sees something finishable.
    static func score(_ task: AvenorTask, now: Date = Date(), calendar: Calendar = .current) -> Double {
        var urgency = 0.75
        if let due = task.dueDate {
            let days = daysBetween(now, due, calendar: calendar)
            switch days {
            case ..<0: urgency = 6
            case 0: urgency = 5
            case 1: urgency = 4
            case 2...3: urgency = 3
            case 4...7: urgency = 2
            default: urgency = 1
            }
        }
        let remaining = max(task.estimatedMinutes - task.focusedMinutes, 0)
        let quickWin = remaining <= 15 ? 0.5 : 0
        return urgency * 2 + task.priority.weight * 1.5 + quickWin
    }

    static func ranked(_ tasks: [AvenorTask], now: Date = Date(), calendar: Calendar = .current) -> [AvenorTask] {
        tasks.filter { !$0.completed }.sorted { a, b in
            let sa = score(a, now: now, calendar: calendar)
            let sb = score(b, now: now, calendar: calendar)
            if sa != sb { return sa > sb }
            return a.createdAt < b.createdAt
        }
    }

    static func bucket(for task: AvenorTask, now: Date = Date(), calendar: Calendar = .current) -> TaskBucket {
        guard let due = task.dueDate else { return .someday }
        let days = daysBetween(now, due, calendar: calendar)
        switch days {
        case ..<0: return .overdue
        case 0: return .today
        case 1: return .tomorrow
        case 2...6: return .week
        default: return .later
        }
    }

    /// Miller's Law: big work is chunked into focus-sized sessions instead of one scary number.
    static func sessions(forMinutes minutes: Int, sessionLength: Int) -> Int {
        let length = max(sessionLength, 5)
        return max(1, Int((Double(max(minutes, 1)) / Double(length)).rounded(.up)))
    }

    static func dueLabel(_ due: Date, now: Date = Date(), calendar: Calendar = .current) -> String {
        let days = daysBetween(now, due, calendar: calendar)
        switch days {
        case ..<0: return "Overdue · \(due.formatted(.dateTime.month(.abbreviated).day()))"
        case 0: return "Today"
        case 1: return "Tomorrow"
        case 2...6: return due.formatted(.dateTime.weekday(.wide))
        default: return due.formatted(.dateTime.month(.abbreviated).day())
        }
    }

    /// One short line explaining *why* the app picked this task (builds trust in the suggestion).
    static func reason(for task: AvenorTask, sessionLength: Int, now: Date = Date(), calendar: Calendar = .current) -> String {
        var parts: [String] = []
        if let due = task.dueDate {
            switch daysBetween(now, due, calendar: calendar) {
            case ..<0: parts.append("Overdue")
            case 0: parts.append("Due today")
            case 1: parts.append("Due tomorrow")
            default: parts.append("Due \(dueLabel(due, now: now, calendar: calendar))")
            }
        }
        if task.priority == .high { parts.append("High priority") }
        let remaining = max(task.estimatedMinutes - task.focusedMinutes, 5)
        let count = sessions(forMinutes: remaining, sessionLength: sessionLength)
        parts.append(count == 1 ? "1 session" : "\(count) sessions")
        return parts.joined(separator: " · ")
    }

    static func daysBetween(_ from: Date, _ to: Date, calendar: Calendar) -> Int {
        calendar.dateComponents([.day], from: calendar.startOfDay(for: from), to: calendar.startOfDay(for: to)).day ?? 0
    }
}

// MARK: - Quick add: "Bio essay fri 45m !" → a fully filled-in task

struct QuickAddResult: Equatable {
    var title: String = ""
    var dueDate: Date? = nil
    var minutes: Int? = nil
    var priority: TaskPriority? = nil
    var classID: UUID? = nil
    var className: String? = nil
}

enum QuickAddParser {
    private static let fullWeekdays: [String: Int] = [
        "sunday": 1, "monday": 2, "tuesday": 3, "wednesday": 4, "thursday": 5, "friday": 6, "saturday": 7
    ]
    private static let shortWeekdays: [String: Int] = [
        "sun": 1, "mon": 2, "tue": 3, "tues": 3, "wed": 4, "weds": 4,
        "thu": 5, "thur": 5, "thurs": 5, "fri": 6, "sat": 7
    ]

    static func parse(_ input: String, classes: [AvenorClass], now: Date = Date(), calendar: Calendar = .current) -> QuickAddResult {
        var result = QuickAddResult()
        var titleWords: [String] = []
        let words = input.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        var i = 0
        while i < words.count {
            let raw = words[i]
            let word = raw.lowercased().trimmingCharacters(in: CharacterSet(charactersIn: ",.;"))
            if ["!", "!!", "!!!", "!high", "!h"].contains(word) {
                result.priority = .high
            } else if ["!low", "!l"].contains(word) {
                result.priority = .low
            } else if ["!med", "!medium", "!m"].contains(word) {
                result.priority = .medium
            } else if let minutes = Self.duration(word) {
                result.minutes = minutes
            } else if word.hasPrefix("#"), word.count > 1, let match = matchClass(String(word.dropFirst()), in: classes) {
                result.classID = match.id
                result.className = match.name
            } else if ["due", "by", "on"].contains(word), i + 1 < words.count,
                      let parsedDate = Self.date(from: words[i + 1], now: now, calendar: calendar) {
                result.dueDate = parsedDate
                i += 1
            } else if let parsedDate = Self.date(from: raw, now: now, calendar: calendar) {
                result.dueDate = parsedDate
            } else {
                titleWords.append(raw)
            }
            i += 1
        }
        result.title = titleWords.joined(separator: " ")

        // Auto-tag the class when its name appears in the title ("Bio lab report" → Bio).
        if result.classID == nil {
            let lowerTitle = result.title.lowercased()
            let titleTokens = Set(lowerTitle.split(whereSeparator: { !$0.isLetter && !$0.isNumber }).map(String.init))
            if let match = classes.first(where: { cls in
                let name = cls.name.lowercased()
                return titleTokens.contains(name) || (name.count >= 4 && lowerTitle.contains(name))
                    || titleTokens.contains { $0.count >= 3 && name.hasPrefix($0) }   // "bio" → Biology
            }) {
                result.classID = match.id
                result.className = match.name
            }
        }
        return result
    }

    static func duration(_ word: String) -> Int? {
        let units: [(String, Double)] = [("mins", 1), ("min", 1), ("m", 1), ("hrs", 60), ("hr", 60), ("h", 60)]
        for (suffix, multiplier) in units where word.hasSuffix(suffix) {
            let number = word.dropLast(suffix.count)
            if let value = Double(number), value > 0, value * multiplier <= 600 {
                return Int((value * multiplier).rounded())
            }
        }
        return nil
    }

    static func date(from rawWord: String, now: Date, calendar: Calendar) -> Date? {
        let word = rawWord.lowercased().trimmingCharacters(in: CharacterSet(charactersIn: ",.;"))
        let today = calendar.startOfDay(for: now)
        switch word {
        case "today", "tonight": return today
        case "tomorrow", "tmrw", "tmr": return calendar.date(byAdding: .day, value: 1, to: today)
        default: break
        }
        // Abbreviations only count when typed lowercase, so "SAT prep" stays a title, not Saturday.
        let isAllCaps = rawWord.count > 1 && rawWord == rawWord.uppercased()
        if let weekday = fullWeekdays[word] ?? (isAllCaps ? nil : shortWeekdays[word]) {
            let current = calendar.component(.weekday, from: today)
            let delta = (weekday - current + 7) % 7
            return calendar.date(byAdding: .day, value: delta, to: today)
        }
        // 10/30 or 10/30/26
        let pieces = word.split(separator: "/")
        let numbers = pieces.compactMap { Int($0) }
        guard pieces.count >= 2, pieces.count <= 3, numbers.count == pieces.count else { return nil }
        let month = numbers[0], day = numbers[1]
        guard (1...12).contains(month), (1...31).contains(day) else { return nil }
        var components = DateComponents()
        components.month = month
        components.day = day
        if numbers.count == 3 {
            components.year = numbers[2] < 100 ? 2000 + numbers[2] : numbers[2]
            return calendar.date(from: components)
        }
        components.year = calendar.component(.year, from: today)
        guard let date = calendar.date(from: components) else { return nil }
        return date < today ? calendar.date(byAdding: .year, value: 1, to: date) : date
    }

    private static func matchClass(_ tag: String, in classes: [AvenorClass]) -> AvenorClass? {
        classes.first { cls in
            let name = cls.name.lowercased()
            return name.hasPrefix(tag) || name.replacingOccurrences(of: " ", with: "").hasPrefix(tag)
        }
    }
}

// MARK: - Spaced repetition (Leitner boxes)

enum Leitner {
    /// Days until the next review for each box.
    static let intervals: [Int: Int] = [1: 1, 2: 2, 3: 4, 4: 8, 5: 16]

    /// - Parameter relearning: the card was already missed earlier in this session.
    static func review(_ card: Flashcard, remembered: Bool, relearning: Bool = false,
                       now: Date = Date(), calendar: Calendar = .current) -> Flashcard {
        var updated = card
        updated.lastReviewed = now
        let today = calendar.startOfDay(for: now)
        if remembered {
            updated.box = relearning ? 1 : min(card.box + 1, 5)
            let days = intervals[updated.box] ?? 1
            updated.dueAt = calendar.date(byAdding: .day, value: days, to: today) ?? now
        } else {
            updated.box = 1
            updated.lapses += 1
            updated.dueAt = now.addingTimeInterval(10 * 60)
        }
        return updated
    }
}

// MARK: - Streaks (kind version)

enum Streaks {
    /// Consecutive active days ending today (or yesterday, so the streak isn't "lost" before you've had a chance today).
    /// One missed day is forgiven per week of streak — losing a 40-day streak to one sick day makes students quit the app.
    static func current(activeDays: Set<Date>, now: Date = Date(), calendar: Calendar = .current) -> Int {
        let today = calendar.startOfDay(for: now)
        guard let yesterday = calendar.date(byAdding: .day, value: -1, to: today) else { return 0 }
        var day = activeDays.contains(today) ? today : yesterday
        var count = 0
        var gracesUsed = 0
        for _ in 0..<3650 {
            if activeDays.contains(day) {
                count += 1
            } else {
                let previous = calendar.date(byAdding: .day, value: -1, to: day) ?? day
                let allowed = 1 + count / 7
                if count > 0, gracesUsed < allowed, activeDays.contains(previous) {
                    gracesUsed += 1
                } else {
                    break
                }
            }
            guard let next = calendar.date(byAdding: .day, value: -1, to: day) else { break }
            day = next
        }
        return count
    }
}

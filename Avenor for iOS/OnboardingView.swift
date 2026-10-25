import SwiftUI
import Foundation

/// Three short steps, each with one decision and a pre-filled answer.
/// Tesler's Law: the setup work (colors, reminder times, session lengths) is done for the student.
struct OnboardingView: View {
    @EnvironmentObject private var store: AvenorStore
    @State private var step = 0
    @State private var name = ""
    @State private var grade: GradeLevel = .g9
    @State private var goal = 60
    @State private var classInput = ""
    @FocusState private var typing: Bool

    private let suggestions = ["Math", "English", "Biology", "Chemistry", "Physics", "History", "Spanish", "Computer Science"]

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { index in
                    Capsule()
                        .fill(index <= step ? AvenorTheme.accent : AvenorTheme.elevated)
                        .frame(height: 5)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .accessibilityLabel("Step \(step + 1) of 3")

            ScrollView {
                Group {
                    switch step {
                    case 0: welcomeStep
                    case 1: classesStep
                    default: goalStep
                    }
                }
                .padding(24)
                .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                                        removal: .opacity))
            }
            .scrollDismissesKeyboard(.interactively)

            VStack(spacing: 8) {
                Button(action: next) {
                    Text(step == 2 ? "Start studying" : "Continue")
                }
                .buttonStyle(PrimaryButtonStyle())
                if step > 0 {
                    Button("Back") { withAnimation(.snappy) { step -= 1 } }
                        .font(.subheadline.weight(.semibold))
                        .frame(minHeight: AvenorTheme.minTap)
                        .foregroundStyle(AvenorTheme.muted)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 12)
        }
        .background(AvenorTheme.background.ignoresSafeArea())
    }

    // MARK: Steps

    private var welcomeStep: some View {
        VStack(alignment: .leading, spacing: 22) {
            Image(systemName: "sparkles")
                .font(.system(size: 44))
                .foregroundStyle(AvenorTheme.accent)
            VStack(alignment: .leading, spacing: 8) {
                Text("Welcome to Avenor").font(.largeTitle.bold())
                Text("Your planner, focus timer and memory coach — free, private, and built by students.")
                    .font(.title3)
                    .foregroundStyle(AvenorTheme.muted)
            }
            VStack(alignment: .leading, spacing: 8) {
                Text("What should we call you?").font(.headline)
                TextField("First name", text: $name)
                    .textContentType(.givenName)
                    .focused($typing)
                    .padding(14)
                    .frame(minHeight: 52)
                    .background(AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            VStack(alignment: .leading, spacing: 8) {
                Text("What grade are you in?").font(.headline)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 100), spacing: 10)], spacing: 10) {
                    ForEach(GradeLevel.allCases) { level in
                        Button {
                            grade = level
                            goal = level.suggestedDailyGoal
                        } label: {
                            Text(level.rawValue)
                                .font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity, minHeight: 48)
                                .foregroundStyle(grade == level ? Color.black : Color.white)
                                .background(grade == level ? AvenorTheme.accent : AvenorTheme.surface,
                                            in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(grade == level ? .isSelected : [])
                    }
                }
            }
        }
    }

    private var classesStep: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                Text("What are you taking?").font(.largeTitle.bold())
                Text("Tap to add. Each class gets its own color so you can spot it anywhere.")
                    .foregroundStyle(AvenorTheme.muted)
            }
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 10)], spacing: 10) {
                ForEach(suggestions, id: \.self) { subject in
                    let added = store.classes.contains { $0.name.caseInsensitiveCompare(subject) == .orderedSame }
                    Button { toggleClass(subject) } label: {
                        HStack {
                            Image(systemName: added ? "checkmark.circle.fill" : "plus.circle")
                            Text(subject).lineLimit(1)
                            Spacer(minLength: 0)
                        }
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 12)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .foregroundStyle(added ? Color.black : Color.white)
                        .background(added ? AvenorTheme.accent : AvenorTheme.surface,
                                    in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            HStack(spacing: 8) {
                TextField("Add another class", text: $classInput)
                    .focused($typing)
                    .submitLabel(.done)
                    .onSubmit(addTypedClass)
                    .padding(14)
                    .frame(minHeight: 52)
                    .background(AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                Button(action: addTypedClass) {
                    Image(systemName: "plus")
                        .font(.headline)
                        .frame(width: 52, height: 52)
                        .background(AvenorTheme.elevated, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .accessibilityLabel("Add class")
            }
            if !store.classes.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(store.classes) { cls in
                        HStack {
                            Circle().fill(AvenorTheme.classColor(cls.colorIndex)).frame(width: 10, height: 10)
                            Text(cls.name)
                            Spacer()
                            Button { store.deleteClass(id: cls.id) } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundStyle(AvenorTheme.muted)
                                    .frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                            }
                            .accessibilityLabel("Remove \(cls.name)")
                        }
                    }
                }
            }
            Text("You can skip this and add classes later.")
                .font(.footnote)
                .foregroundStyle(AvenorTheme.muted)
        }
    }

    private var goalStep: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                Text("How much focus a day?").font(.largeTitle.bold())
                Text("Pick something easy to hit. You can change it anytime.")
                    .foregroundStyle(AvenorTheme.muted)
            }
            ForEach([30, 60, 90, 120], id: \.self) { minutes in
                Button { goal = minutes } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(Self.goalTitle(minutes))
                                .font(.headline)
                            Text(Self.goalBlurb(minutes)).font(.caption).foregroundStyle(goal == minutes ? Color.black.opacity(0.7) : AvenorTheme.muted)
                        }
                        Spacer()
                        if minutes == grade.suggestedDailyGoal { Chip("Suggested", color: goal == minutes ? .black : AvenorTheme.accent) }
                    }
                    .padding(16)
                    .frame(minHeight: 64)
                    .foregroundStyle(goal == minutes ? Color.black : Color.white)
                    .background(goal == minutes ? AvenorTheme.accent : AvenorTheme.surface,
                                in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            Panel {
                VStack(alignment: .leading, spacing: 6) {
                    Label("Turn on reminders?", systemImage: "bell.badge.fill").font(.headline)
                    Text("Avenor reminds you the night before something is due and when a focus session ends. Nothing else, ever.")
                        .font(.subheadline)
                        .foregroundStyle(AvenorTheme.muted)
                    Toggle("Reminders", isOn: Binding(
                        get: { store.profile.remindersEnabled },
                        set: { newValue in Task { await store.setReminders(newValue) } }
                    ))
                    .tint(AvenorTheme.accent)
                }
            }
        }
    }

    private static func goalTitle(_ minutes: Int) -> String {
        switch minutes {
        case 30: return "30 minutes"
        case 60: return "1 hour"
        case 90: return "1½ hours"
        default: return "\(minutes / 60) hours"
        }
    }

    private static func goalBlurb(_ minutes: Int) -> String {
        switch minutes {
        case ..<45: return "One or two sessions. Great for building the habit."
        case ..<75: return "About two solid sessions a day."
        case ..<105: return "Three sessions — strong for busy high schoolers."
        default: return "Four-plus sessions — college-level study load."
        }
    }

    // MARK: Actions

    private func toggleClass(_ subject: String) {
        if let existing = store.classes.first(where: { $0.name.caseInsensitiveCompare(subject) == .orderedSame }) {
            store.deleteClass(id: existing.id)
        } else {
            store.addClass(AvenorClass(name: subject, colorIndex: store.classes.count))
        }
    }

    private func addTypedClass() {
        let clean = classInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        if !store.classes.contains(where: { $0.name.caseInsensitiveCompare(clean) == .orderedSame }) {
            store.addClass(AvenorClass(name: clean, colorIndex: store.classes.count))
        }
        classInput = ""
    }

    private func next() {
        typing = false
        if step < 2 {
            withAnimation(.snappy) { step += 1 }
            return
        }
        var profile = store.profile
        profile.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        profile.grade = grade
        profile.dailyGoalMinutes = goal
        profile.defaultSessionMinutes = grade == .g8 ? 20 : 25
        profile.onboarded = true
        // A first card that teaches how Review works by being an example of it.
        if store.cards.isEmpty {
            store.addCard(Flashcard(
                front: "What are the two study habits that make memories last?",
                back: "Testing yourself (retrieval practice) and spacing reviews out over days. Avenor's Review tab does both for you."
            ))
        }
        withAnimation(.easeInOut) { store.profile = profile }
        store.resyncReminders()
    }
}

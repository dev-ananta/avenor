import SwiftUI
import Foundation

/// Study Mode setup. Two questions only — "what?" and "how long?" — with the answer to both pre-filled.
struct FocusScreen: View {
    @EnvironmentObject private var store: AvenorStore
    @EnvironmentObject private var focus: FocusEngine
    @State private var selectedTaskID: UUID?
    @State private var customLabel = ""
    @State private var minutes = 25
    @State private var didInit = false

    /// Hick's Law: 3 presets (plus your default) instead of a free-form number picker.
    private var presets: [Int] { Array(Set([15, 25, 45, store.profile.defaultSessionMinutes])).sorted() }
    /// Miller's Law: never more than 5 candidates.
    private var candidates: [AvenorTask] { Array(store.rankedActive.prefix(5)) }

    private var label: String {
        let typed = customLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        if !typed.isEmpty { return typed }
        return store.task(id: selectedTaskID)?.title ?? "Free study"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    ScreenTitle(title: "Focus", subtitle: "One task. One timer. Phone face-down.")
                    whatSection
                    howLongSection
                    whyItWorks
                    if !store.sessions.isEmpty { historySection }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(AvenorTheme.background.ignoresSafeArea())
            .safeAreaInset(edge: .bottom) {
                // Fitts's Law: the one action that matters is huge and sits in the thumb zone.
                Button {
                    let taskID = customLabel.trimmingCharacters(in: .whitespaces).isEmpty ? selectedTaskID : nil
                    focus.start(taskID: taskID, label: label, minutes: minutes)
                } label: {
                    Label("Start \(minutes)-min session", systemImage: "play.fill")
                }
                .buttonStyle(PrimaryButtonStyle())
                .padding(.horizontal, 20)
                .padding(.bottom, 10)
                .padding(.top, 6)
                .background(AvenorTheme.background.opacity(0.95))
            }
            .onAppear {
                guard !didInit else { return }
                minutes = store.profile.defaultSessionMinutes
                selectedTaskID = candidates.first?.id
                didInit = true
            }
        }
    }

    private var whatSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("What are you working on?").font(.headline)
            ForEach(Array(candidates.enumerated()), id: \.element.id) { index, task in
                let isSelected = selectedTaskID == task.id && customLabel.isEmpty
                Button {
                    selectedTaskID = task.id
                    customLabel = ""
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                            .font(.title3)
                            .foregroundStyle(isSelected ? AvenorTheme.accent : AvenorTheme.muted)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(task.title).font(.body.weight(.semibold)).foregroundStyle(.white).lineLimit(1)
                            Text(Planner.reason(for: task, sessionLength: minutes))
                                .font(.caption).foregroundStyle(AvenorTheme.muted)
                        }
                        Spacer()
                        if index == 0 { Chip("Suggested", color: AvenorTheme.accent) }
                    }
                    .padding(14)
                    .frame(minHeight: 56)
                    .background(isSelected ? AvenorTheme.accent.opacity(0.12) : AvenorTheme.surface,
                                in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(isSelected ? AvenorTheme.accent : AvenorTheme.border, lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
            TextField(candidates.isEmpty ? "What are you studying?" : "Or type something else…", text: $customLabel)
                .padding(14)
                .frame(minHeight: 52)
                .background(AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(AvenorTheme.border, lineWidth: 1))
        }
    }

    private var howLongSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("How long?").font(.headline)
            HStack(spacing: 10) {
                ForEach(presets, id: \.self) { preset in
                    Button { minutes = preset } label: {
                        VStack(spacing: 2) {
                            Text("\(preset)").font(.title3.bold())
                            Text("min").font(.caption)
                        }
                        .frame(maxWidth: .infinity, minHeight: 64)
                        .foregroundStyle(minutes == preset ? Color.black : Color.white)
                        .background(minutes == preset ? AvenorTheme.accent : AvenorTheme.surface,
                                    in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(minutes == preset ? .isSelected : [])
                }
            }
            Text(minutes <= 15 ? "Great for getting unstuck or a quick review."
                 : minutes <= 30 ? "The sweet spot for most homework."
                 : "Deep work — best for essays, projects and test prep.")
                .font(.footnote)
                .foregroundStyle(AvenorTheme.muted)
            Button {
                let taskID = customLabel.trimmingCharacters(in: .whitespaces).isEmpty ? selectedTaskID : nil
                focus.start(taskID: taskID, label: label, minutes: 5)
            } label: {
                Label("Stuck? Just start with 5 minutes", systemImage: "bolt.fill")
                    .font(.subheadline.weight(.semibold))
                    .frame(minHeight: AvenorTheme.minTap)
            }
        }
    }

    private var whyItWorks: some View {
        Panel {
            VStack(alignment: .leading, spacing: 8) {
                Label("How a round works", systemImage: "brain.head.profile").font(.headline)
                Text("1. Focus on one thing until the ring fills.\n2. Write one question you can now answer — Avenor turns it into a review card.\n3. Take a real break (no scrolling), then go again.")
                    .font(.subheadline)
                    .foregroundStyle(AvenorTheme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "Recent sessions", trailing: "\(store.todayFocusMinutes) min today")
            Panel {
                VStack(spacing: 10) {
                    ForEach(store.sessions.suffix(4).reversed()) { session in
                        HStack {
                            Image(systemName: session.reachedGoal ? "checkmark.seal.fill" : "leaf.fill")
                                .foregroundStyle(session.reachedGoal ? AvenorTheme.green : AvenorTheme.accent)
                            Text(session.label).lineLimit(1)
                            Spacer()
                            Text("\(session.focusedMinutes) min").foregroundStyle(AvenorTheme.muted)
                        }
                        .font(.subheadline)
                    }
                }
            }
        }
    }
}

// MARK: - The session itself (full screen, nothing else to tap)

struct FocusSessionView: View {
    @EnvironmentObject private var store: AvenorStore
    @EnvironmentObject private var focus: FocusEngine
    @State private var question = ""
    @State private var answer = ""
    @State private var markTaskDone = false
    @State private var celebrate = 0

    private var currentTask: AvenorTask? { store.task(id: focus.taskID) }

    var body: some View {
        ZStack {
            AvenorTheme.background.ignoresSafeArea()
            switch focus.phase {
            case .focusing, .paused: runningView
            case .finished: finishedView
            case .onBreak: breakView
            case .idle: Color.clear
            }
        }
        .animation(.spring(response: 0.45, dampingFraction: 0.85), value: focus.phase)
        .task(id: focus.phase) { await watchClock() }
        .sensoryFeedback(.success, trigger: celebrate)
    }

    // MARK: Running

    private var runningView: some View {
        VStack(spacing: 24) {
            VStack(spacing: 6) {
                Text("FOCUSING ON")
                    .font(.caption.weight(.heavy)).tracking(1.5)
                    .foregroundStyle(AvenorTheme.muted)
                Text(focus.label)
                    .font(.title3.bold())
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
            }
            .padding(.top, 24)
            Spacer()
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let progress = focus.progress(at: context.date)
                ZStack {
                    ProgressRing(progress: progress, lineWidth: 16,
                                 tint: focus.phase == .paused ? AvenorTheme.muted : AvenorTheme.accent)
                    VStack(spacing: 8) {
                        Text(Clock.string(focus.remaining(at: context.date)))
                            .font(.system(size: 64, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText(countsDown: true))
                        Text(focus.phase == .paused ? "Paused" : Self.encouragement(progress))
                            .font(.subheadline)
                            .foregroundStyle(AvenorTheme.muted)
                    }
                }
                .frame(width: 280, height: 280)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(Int(focus.remaining(at: context.date) / 60)) minutes remaining")
            }
            Spacer()
            Text("Phone face-down. Notifications can wait.")
                .font(.footnote)
                .foregroundStyle(AvenorTheme.muted)
            Button {
                if focus.phase == .paused { focus.resume() } else { focus.pause() }
            } label: {
                Label(focus.phase == .paused ? "Resume" : "Pause",
                      systemImage: focus.phase == .paused ? "play.fill" : "pause.fill")
            }
            .buttonStyle(PrimaryButtonStyle())
            HoldToEndButton { endEarly() }
        }
        .padding(24)
    }

    /// Goal-gradient: the closer you are, the more the copy pulls you toward the finish.
    private static func encouragement(_ progress: Double) -> String {
        switch progress {
        case ..<0.15: return "Settle in. Just this one thing."
        case ..<0.5: return "You're in the zone."
        case ..<0.75: return "Past halfway — keep going."
        default: return "Final stretch!"
        }
    }

    // MARK: Finished — Peak-End Rule: make the last moment the best one

    private var finishedView: some View {
        let session = focus.lastSession
        let reached = session?.reachedGoal ?? false
        let minutesDone = session?.focusedMinutes ?? 0
        return ScrollView {
            VStack(spacing: 20) {
                Image(systemName: reached ? "checkmark.seal.fill" : "leaf.fill")
                    .font(.system(size: 76))
                    .foregroundStyle(reached ? AvenorTheme.green : AvenorTheme.accent)
                    .symbolEffect(.bounce, value: celebrate)
                    .padding(.top, 32)
                Text(reached ? "Session complete!" : "Nice — that still counts.")
                    .font(.largeTitle.bold())
                    .multilineTextAlignment(.center)
                Text(reached
                     ? "\(minutesDone) focused minutes on \(focus.label)."
                     : "\(minutesDone) focused minutes is real progress. Shorter rounds are fine.")
                    .foregroundStyle(AvenorTheme.muted)
                    .multilineTextAlignment(.center)
                HStack(spacing: 10) {
                    StatTile(value: "\(store.todayFocusMinutes)m", label: "today", color: AvenorTheme.accent)
                    StatTile(value: "\(store.streak)", label: "day streak", color: AvenorTheme.orange)
                    StatTile(value: "\(Int(min(store.dailyGoalProgress, 9.99) * 100))%", label: "daily goal", color: AvenorTheme.green)
                }

                Panel {
                    VStack(alignment: .leading, spacing: 10) {
                        Label("Lock it in · 30 seconds", systemImage: "brain.head.profile").font(.headline)
                        Text("Write one question you can now answer. Avenor will quiz you on it over the next few days so it actually sticks.")
                            .font(.subheadline)
                            .foregroundStyle(AvenorTheme.muted)
                            .fixedSize(horizontal: false, vertical: true)
                        TextField("Question (e.g. What does mitochondria do?)", text: $question, axis: .vertical)
                            .padding(12)
                            .background(AvenorTheme.elevated, in: RoundedRectangle(cornerRadius: 12))
                        TextField("Answer", text: $answer, axis: .vertical)
                            .padding(12)
                            .background(AvenorTheme.elevated, in: RoundedRectangle(cornerRadius: 12))
                    }
                }

                if let task = currentTask, !task.completed {
                    Panel {
                        Toggle(isOn: $markTaskDone) {
                            Text("Mark “\(task.title)” as done").font(.subheadline.weight(.semibold))
                        }
                        .tint(AvenorTheme.green)
                    }
                }

                Button {
                    wrapUp()
                    focus.startBreak(minutes: focus.suggestedBreakMinutes)
                } label: {
                    Label("Take a \(focus.suggestedBreakMinutes)-min break", systemImage: "cup.and.saucer.fill")
                }
                .buttonStyle(PrimaryButtonStyle())

                Button("Done for now") {
                    wrapUp()
                    focus.close()
                }
                .buttonStyle(SecondaryButtonStyle())
            }
            .padding(24)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    // MARK: Break

    private var breakView: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("Break time").font(.largeTitle.bold()).padding(.top, 24)
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    let left = max(0, (focus.breakEndsAt ?? context.date).timeIntervalSince(context.date))
                    Text(left > 0 ? Clock.string(left) : "Ready when you are")
                        .font(.system(size: left > 0 ? 44 : 26, weight: .bold, design: .rounded))
                        .monospacedDigit()
                }
                // Survey: about 1 in 4 students asked for something calming. A 60-second paced-breathing reset.
                BreathingReset()
                Panel {
                    VStack(alignment: .leading, spacing: 12) {
                        Label("Stand up and stretch", systemImage: "figure.walk")
                        Label("Drink some water", systemImage: "drop.fill")
                        Label("Look at something far away", systemImage: "eye")
                    }
                    .font(.subheadline)
                }
                Text("Skip the short videos — they make it much harder to start again.")
                    .font(.footnote)
                    .foregroundStyle(AvenorTheme.muted)
                    .multilineTextAlignment(.center)
                Button {
                    // After a 5-minute starter, the next round grows to the student's normal length.
                    let planned = Int(focus.plannedSeconds / 60)
                    focus.nextRound(minutes: planned <= 5 ? store.profile.defaultSessionMinutes : planned)
                } label: {
                    Label("Start next round", systemImage: "play.fill")
                }
                .buttonStyle(PrimaryButtonStyle())
                Button("End for now") { focus.close() }
                    .buttonStyle(SecondaryButtonStyle())
            }
            .padding(24)
        }
    }

    // MARK: Logic

    private func watchClock() async {
        while !Task.isCancelled {
            if focus.phase == .focusing && focus.remaining() <= 0 {
                finish()
                return
            }
            try? await Task.sleep(for: .milliseconds(500))
        }
    }

    private func finish() {
        if let session = focus.complete() { store.log(session) }
        celebrate += 1
    }

    private func endEarly() {
        if focus.elapsed() < 60 {
            focus.close()
        } else {
            finish()
        }
    }

    private func wrapUp() {
        let q = question.trimmingCharacters(in: .whitespacesAndNewlines)
        let a = answer.trimmingCharacters(in: .whitespacesAndNewlines)
        if !q.isEmpty && !a.isEmpty {
            // Due tomorrow: the first spaced review lands right when forgetting starts.
            let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Calendar.current.startOfDay(for: Date())) ?? Date()
            store.addCard(Flashcard(classID: currentTask?.classID, front: q, back: a, dueAt: tomorrow))
        }
        if markTaskDone, let task = currentTask, !task.completed {
            store.toggle(task)
        }
        question = ""
        answer = ""
        markTaskDone = false
    }
}

/// Fitts's Law used in reverse: quitting is deliberately harder to hit than continuing.
/// A 1.5-second hold stops impulsive quits; VoiceOver users get a normal action.
struct HoldToEndButton: View {
    let action: () -> Void
    @State private var holding = false

    var body: some View {
        Text(holding ? "Keep holding to end…" : "Hold to end early")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(AvenorTheme.muted)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(alignment: .leading) {
                GeometryReader { geo in
                    Capsule()
                        .fill(AvenorTheme.red.opacity(0.25))
                        .frame(width: holding ? geo.size.width : 0)
                        .animation(holding ? .linear(duration: 1.5) : .easeOut(duration: 0.2), value: holding)
                }
            }
            .clipShape(Capsule())
            .contentShape(Capsule())
            .onLongPressGesture(minimumDuration: 1.5, perform: action, onPressingChanged: { pressing in
                holding = pressing
            })
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("End session early")
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { action() }
    }
}

/// Box breathing (4 s in, 4 s hold, 4 s out, 4 s hold) for one minute. A simple, non-clinical way to settle
/// before the next round. Tap to start; it never starts by itself.
struct BreathingReset: View {
    @State private var startedAt: Date?
    private let total: TimeInterval = 64

    var body: some View {
        Panel {
            VStack(spacing: 14) {
                if let startedAt {
                    TimelineView(.animation) { context in
                        let elapsed = context.date.timeIntervalSince(startedAt)
                        let step = Int(elapsed / 4) % 4
                        let within = (elapsed.truncatingRemainder(dividingBy: 4)) / 4
                        let scale: Double = step == 0 ? 0.6 + 0.4 * within : step == 1 ? 1 : step == 2 ? 1 - 0.4 * within : 0.6
                        VStack(spacing: 12) {
                            ZStack {
                                Circle().fill(AvenorTheme.sky.opacity(0.15)).frame(width: 140, height: 140)
                                Circle().fill(AvenorTheme.sky.opacity(0.55)).frame(width: 140 * scale, height: 140 * scale)
                            }
                            Text(elapsed >= total ? "Nice. Ready when you are."
                                 : ["Breathe in", "Hold", "Breathe out", "Hold"][step])
                                .font(.headline)
                        }
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(elapsed >= total ? "Breathing finished" : ["Breathe in", "Hold", "Breathe out", "Hold"][step])
                    }
                    Button("Stop") { self.startedAt = nil }
                        .font(.subheadline)
                        .frame(minHeight: AvenorTheme.minTap)
                } else {
                    Label("60-second breathing reset", systemImage: "wind").font(.headline)
                    Text("Slow breathing helps you settle before the next round.")
                        .font(.subheadline).foregroundStyle(AvenorTheme.muted)
                        .multilineTextAlignment(.center)
                    Button { startedAt = Date() } label: { Label("Start", systemImage: "play.fill") }
                        .buttonStyle(SecondaryButtonStyle())
                }
            }
            .frame(maxWidth: .infinity)
        }
    }
}

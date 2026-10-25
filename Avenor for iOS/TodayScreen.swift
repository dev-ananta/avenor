import SwiftUI
import Foundation

/// The "open the app and know exactly what to do" screen.
/// - Hick's Law: one recommended task and one big Start button instead of a full list to choose from.
/// - Von Restorff: the Focus Now card is the only bright, colored block on the screen.
/// - Miller's Law: at most 3 "up next" tasks and 3 events — the rest lives in Plan.
/// - Goal-gradient + Peak-End: a daily ring that fills up, and an evening wrap-up that ends the day on a win.
struct TodayScreen: View {
    @EnvironmentObject private var store: AvenorStore
    @EnvironmentObject private var focus: FocusEngine
    @State private var skipped: Set<UUID> = []
    @State private var showingProfile = false
    @State private var completedPulse = 0

    private var ranked: [AvenorTask] { store.rankedActive }
    private var hero: AvenorTask? { ranked.first(where: { !skipped.contains($0.id) }) ?? ranked.first }
    private var upNext: [AvenorTask] { Array(ranked.filter { $0.id != hero?.id }.prefix(3)) }
    private var todaysEvents: [AvenorEvent] { Array(store.schedule(on: Date()).prefix(3)) }
    private var hour: Int { Calendar.current.component(.hour, from: Date()) }
    private var sessionMinutes: Int { store.profile.defaultSessionMinutes }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    progressCard
                    focusNowCard
                    if !upNext.isEmpty { upNextSection }
                    if !todaysEvents.isEmpty { scheduleSection }
                    if !store.dueCards.isEmpty { reviewNudge }
                    if hour >= 19 { dayWrap }
                }
                .padding(.horizontal, 20)
                .padding(.top, 4)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(AvenorTheme.background.ignoresSafeArea())
            .safeAreaInset(edge: .bottom) {
                QuickAddBar()
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showingProfile = true } label: {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.title2)
                            .foregroundStyle(AvenorTheme.accent)
                            .frame(minWidth: AvenorTheme.minTap, minHeight: AvenorTheme.minTap)
                    }
                    .accessibilityLabel("Profile and settings")
                }
            }
            .sheet(isPresented: $showingProfile) { YouScreen() }
            .sensoryFeedback(.success, trigger: completedPulse)
        }
    }

    // MARK: Sections

    private var greeting: String {
        let name = store.profile.name.trimmingCharacters(in: .whitespaces)
        let base: String
        switch hour {
        case 5..<12: base = "Good morning"
        case 12..<17: base = "Good afternoon"
        default: base = "Good evening"
        }
        return name.isEmpty ? base : "\(base), \(name)"
    }

    private var header: some View {
        ScreenTitle(title: greeting, subtitle: Date().formatted(.dateTime.weekday(.wide).month(.wide).day()))
    }

    private var goalMessage: String {
        let done = store.todayFocusMinutes
        let goal = store.profile.dailyGoalMinutes
        if done == 0 { return "One session gets you started." }
        if done >= goal { return "Goal reached. Anything more is bonus." }
        if store.dailyGoalProgress >= 0.5 { return "Only \(goal - done) min to today's goal." }
        return "Nice start — \(goal - done) min to go."
    }

    private var progressCard: some View {
        Panel {
            HStack(spacing: 18) {
                ZStack {
                    ProgressRing(progress: store.dailyGoalProgress, lineWidth: 9,
                                 tint: store.dailyGoalProgress >= 1 ? AvenorTheme.green : AvenorTheme.accent)
                    VStack(spacing: 0) {
                        Text("\(store.todayFocusMinutes)")
                            .font(.title3.bold())
                            .contentTransition(.numericText())
                        Text("of \(store.profile.dailyGoalMinutes)m")
                            .font(.caption2)
                            .foregroundStyle(AvenorTheme.muted)
                    }
                }
                .frame(width: 74, height: 74)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(store.todayFocusMinutes) of \(store.profile.dailyGoalMinutes) focus minutes today")

                VStack(alignment: .leading, spacing: 8) {
                    Text(goalMessage).font(.headline)
                    HStack(spacing: 6) {
                        Chip(store.streak == 1 ? "1-day streak" : "\(store.streak)-day streak", color: AvenorTheme.orange, systemImage: "flame.fill")
                        Chip("\(store.tasksDoneToday) done", color: AvenorTheme.green, systemImage: "checkmark")
                    }
                }
            }
        }
    }

    private var focusNowCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("FOCUS NOW")
                .font(.caption.weight(.heavy))
                .tracking(1.5)
                .foregroundStyle(.white.opacity(0.8))
            if let task = hero {
                Text(task.title)
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                    .lineLimit(3)
                Text(Planner.reason(for: task, sessionLength: sessionMinutes))
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.85))
                Button {
                    focus.start(taskID: task.id, label: task.title, minutes: sessionMinutes)
                } label: {
                    Label("Start \(sessionMinutes)-min focus", systemImage: "play.fill")
                }
                .buttonStyle(PrimaryButtonStyle(tint: .white))
                HStack {
                    Button {
                        complete(task)
                    } label: {
                        Label("Done", systemImage: "checkmark")
                            .frame(minHeight: AvenorTheme.minTap)
                    }
                    Spacer()
                    // Survey: 39% of students delay or distract themselves when stressed.
                    // A 5-minute start is small enough to beat avoidance; most keep going once started.
                    Button {
                        focus.start(taskID: task.id, label: task.title, minutes: 5)
                    } label: {
                        Label("Just 5 min", systemImage: "bolt.fill")
                            .frame(minHeight: AvenorTheme.minTap)
                    }
                    Spacer()
                    if ranked.count > 1 {
                        Button {
                            withAnimation(.snappy) { skip(task) }
                        } label: {
                            Label("Not now", systemImage: "arrow.forward")
                                .frame(minHeight: AvenorTheme.minTap)
                        }
                    }
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white.opacity(0.92))
            } else {
                Text(store.tasks.isEmpty ? "Let's set up your first task" : "All caught up!")
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                Text("Add something below, or start a free study session right now.")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.85))
                Button {
                    focus.start(taskID: nil, label: "Free study", minutes: sessionMinutes)
                } label: {
                    Label("Start free focus", systemImage: "play.fill")
                }
                .buttonStyle(PrimaryButtonStyle(tint: .white))
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AvenorTheme.heroGradient, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: AvenorTheme.accent.opacity(0.35), radius: 18, y: 8)
    }

    private var upNextSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "Up next", trailing: ranked.count > 4 ? "+\(ranked.count - 4) more" : nil)
            Panel {
                VStack(spacing: 6) {
                    ForEach(upNext) { task in
                        TaskRowView(task: task) { complete(task) }
                        if task.id != upNext.last?.id { Divider().overlay(AvenorTheme.border) }
                    }
                }
            }
            Button {
                store.tab = .plan
            } label: {
                Label("See your whole plan", systemImage: "list.bullet")
                    .font(.subheadline.weight(.semibold))
                    .frame(minHeight: AvenorTheme.minTap)
            }
        }
    }

    private var scheduleSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "Today's schedule")
            Panel {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(todaysEvents) { event in
                        HStack(spacing: 12) {
                            RoundedRectangle(cornerRadius: 2)
                                .fill(store.color(forClassID: event.classID))
                                .frame(width: 4, height: 36)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(event.title).font(.headline)
                                Text(event.timeLabel ?? "All day").font(.subheadline).foregroundStyle(AvenorTheme.muted)
                            }
                        }
                    }
                }
            }
        }
    }

    private var reviewNudge: some View {
        let count = store.dueCards.count
        return Button {
            store.tab = .review
        } label: {
            Panel {
                HStack(spacing: 14) {
                    Image(systemName: "brain.head.profile")
                        .font(.title2)
                        .foregroundStyle(AvenorTheme.violet)
                        .frame(width: 44, height: 44)
                        .background(AvenorTheme.violet.opacity(0.15), in: RoundedRectangle(cornerRadius: 12))
                    VStack(alignment: .leading, spacing: 3) {
                        Text(count == 1 ? "1 card ready to review" : "\(count) cards ready to review").font(.headline)
                        Text("About \(max(1, count / 4)) min · keeps today's learning from fading")
                            .font(.caption).foregroundStyle(AvenorTheme.muted)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").foregroundStyle(AvenorTheme.muted)
                }
            }
        }
        .buttonStyle(.plain)
    }

    /// Peak-End Rule applied to the whole day: end on what went right, and pre-decide tomorrow's first move.
    private var dayWrap: some View {
        let tomorrowFirst = ranked.first
        return VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "Today's wrap")
            Panel {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 10) {
                        StatTile(value: "\(store.todayFocusMinutes)m", label: "focused", color: AvenorTheme.accent)
                        StatTile(value: "\(store.tasksDoneToday)", label: "tasks done", color: AvenorTheme.green)
                        StatTile(value: "\(store.reviewsToday)", label: "cards", color: AvenorTheme.violet)
                    }
                    if store.todayFocusMinutes + store.tasksDoneToday + store.reviewsToday == 0 {
                        Text("Quiet day — that's okay. Even 10 minutes now keeps your streak alive.")
                            .font(.subheadline).foregroundStyle(AvenorTheme.muted)
                    } else {
                        Text("You showed up today. That's the whole game.")
                            .font(.subheadline.weight(.semibold))
                    }
                    if let next = tomorrowFirst {
                        Label("Tomorrow starts with: \(next.title)", systemImage: "sunrise.fill")
                            .font(.subheadline)
                            .foregroundStyle(AvenorTheme.orange)
                    }
                }
            }
        }
    }

    // MARK: Actions

    private func complete(_ task: AvenorTask) {
        withAnimation(.snappy) { store.toggle(task) }
        completedPulse += 1
    }

    private func skip(_ task: AvenorTask) {
        skipped.insert(task.id)
        if ranked.allSatisfy({ skipped.contains($0.id) }) { skipped.removeAll() }
    }
}

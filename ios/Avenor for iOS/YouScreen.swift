import SwiftUI
import Foundation
import StoreKit

struct YouScreen: View {
    @EnvironmentObject private var store: AvenorStore
    @Environment(\.dismiss) private var dismiss
    @State private var confirmReset = false

    private var remindersBinding: Binding<Bool> {
        Binding(
            get: { store.profile.remindersEnabled },
            set: { newValue in Task { await store.setReminders(newValue) } }
        )
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack(spacing: 14) {
                        Circle()
                            .fill(AvenorTheme.heroGradient)
                            .frame(width: 56, height: 56)
                            .overlay(
                                Text(String(store.profile.name.prefix(1)).uppercased())
                                    .font(.title2.bold())
                                    .foregroundStyle(.white)
                            )
                        VStack(alignment: .leading, spacing: 3) {
                            Text(store.profile.name.isEmpty ? "Student" : store.profile.name).font(.headline)
                            Text("\(store.profile.grade.rawValue) · \(store.streak)-day streak")
                                .font(.subheadline).foregroundStyle(AvenorTheme.muted)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section("You") {
                    TextField("Name", text: $store.profile.name)
                    Picker("Grade", selection: $store.profile.grade) {
                        ForEach(GradeLevel.allCases) { Text($0.rawValue).tag($0) }
                    }
                }

                Section {
                    Stepper("Daily focus goal: \(store.profile.dailyGoalMinutes) min",
                            value: $store.profile.dailyGoalMinutes, in: 15...300, step: 15)
                    Picker("Default session", selection: $store.profile.defaultSessionMinutes) {
                        ForEach([15, 20, 25, 30, 45, 50], id: \.self) { Text("\($0) min").tag($0) }
                    }
                } header: {
                    Text("Daily rhythm")
                } footer: {
                    Text("Start small. A goal you hit every day beats a big one you miss.")
                }

                Section {
                    Toggle("Reminders", isOn: remindersBinding)
                    if store.profile.remindersEnabled {
                        Picker("Daily planning nudge", selection: $store.profile.planReminderHour) {
                            ForEach(12...21, id: \.self) { hour in Text(Self.hourLabel(hour)).tag(hour) }
                        }
                        Toggle("Nudge me if I leave a session", isOn: $store.profile.nudgeWhenLeaving)
                    }
                } header: {
                    Text("Reminders")
                } footer: {
                    Text("Due-date reminders arrive at 7 PM the night before and 7 AM the day of. Everything is scheduled on your phone.")
                }

                Section("Your stats") {
                    LabeledContent("Total focus", value: "\(store.totalFocusMinutes / 60)h \(store.totalFocusMinutes % 60)m")
                    LabeledContent("Sessions", value: "\(store.sessions.count)")
                    LabeledContent("Tasks completed", value: "\(store.tasks.filter(\.completed).count)")
                    LabeledContent("Cards mastered", value: "\(store.cards.filter { $0.box >= 4 }.count) of \(store.cards.count)")
                }

                Section {
                    NavigationLink {
                        TipJarView()
                    } label: {
                        Label("Support Avenor", systemImage: "heart.fill")
                    }
                } footer: {
                    Text("Avenor is free — no ads, no subscriptions, no accounts. Made by a Congressional App Challenge team.")
                }

                // Research (Wells, Stanford HAI, 2025): AI chatbots can miss signs of crisis.
                // Avenor deliberately points students to real people instead of an AI counselor.
                Section {
                    Link(destination: URL(string: "tel:988")!) {
                        Label("Call 988 — Suicide & Crisis Lifeline", systemImage: "phone.fill")
                    }
                    Link(destination: URL(string: "sms:988")!) {
                        Label("Text 988", systemImage: "message.fill")
                    }
                } header: {
                    Text("Need to talk to someone?")
                } footer: {
                    Text("Free, 24/7, anywhere in the U.S. You can also talk to a parent, teacher or school counselor. Avenor is a study tool, not a substitute for real support.")
                }

                Section {
                    Label("Your data never leaves this device.", systemImage: "lock.fill")
                        .font(.subheadline)
                    Button("Reset all data", role: .destructive) { confirmReset = true }
                }
            }
            .navigationTitle("You")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
            }
            .onChange(of: store.profile.planReminderHour) { store.resyncReminders() }
            .confirmationDialog("Erase everything?", isPresented: $confirmReset, titleVisibility: .visible) {
                Button("Erase all tasks, cards and history", role: .destructive) {
                    store.resetAll()
                    dismiss()
                }
            } message: {
                Text("This can't be undone.")
            }
        }
    }

    private static func hourLabel(_ hour: Int) -> String {
        let date = Calendar.current.date(bySettingHour: hour, minute: 0, second: 0, of: Date()) ?? Date()
        return date.formatted(date: .omitted, time: .shortened)
    }
}

// MARK: - Tip jar (the only revenue stream — optional, never gates a feature)

enum TipJar {
    /// Create these as *consumable* in-app purchases in App Store Connect.
    static let productIDs = ["avenor.tip.small", "avenor.tip.medium", "avenor.tip.large"]

    /// Finishes any tip transactions that complete while the app is closed.
    static func listenForTransactions() async {
        for await update in StoreKit.Transaction.updates {
            if case .verified(let transaction) = update {
                await transaction.finish()
            }
        }
    }
}

struct TipJarView: View {
    @State private var thanked = false

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                Image(systemName: thanked ? "heart.circle.fill" : "heart.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(AvenorTheme.pink)
                    .symbolEffect(.bounce, value: thanked)
                    .padding(.top, 20)
                Text(thanked ? "Thank you!" : "Keep Avenor free")
                    .font(.title.bold())
                Text("Every feature is unlocked for everyone, forever. Tips help cover the yearly Apple developer fee so the app can stay on the App Store for students who can't pay.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(AvenorTheme.muted)
                ForEach(TipJar.productIDs, id: \.self) { id in
                    ProductView(id: id)
                        .productViewStyle(.compact)
                        .padding(14)
                        .background(AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                Text("Tips are optional and don't unlock anything. Ask a parent or guardian before buying.")
                    .font(.footnote)
                    .foregroundStyle(AvenorTheme.muted)
                    .multilineTextAlignment(.center)
            }
            .padding(20)
        }
        .background(AvenorTheme.background.ignoresSafeArea())
        .navigationTitle("Support")
        .onInAppPurchaseCompletion { _, result in
            if case .success(.success(let verification)) = result, case .verified(let transaction) = verification {
                await transaction.finish()
                await MainActor.run { thanked = true }
            }
        }
    }
}

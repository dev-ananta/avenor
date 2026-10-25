import SwiftUI
import Foundation

/// Retrieval practice + spacing: the two most reliable ways to make studying stick.
/// Cards come back after 1, 2, 4, 8 and 16 days — right before you'd forget them.
private struct CardGroup: Identifiable {
    let id: String
    let title: String
    let color: Color
    let cards: [Flashcard]
}

struct ReviewScreen: View {
    @EnvironmentObject private var store: AvenorStore
    @State private var reviewing = false
    @State private var showingNew = false
    @State private var editing: Flashcard?

    private var groups: [CardGroup] {
        var result: [CardGroup] = store.classes.compactMap { cls in
            let cards = store.cards.filter { $0.classID == cls.id }.sorted { $0.dueAt < $1.dueAt }
            return cards.isEmpty ? nil : CardGroup(id: cls.id.uuidString, title: cls.name, color: AvenorTheme.classColor(cls.colorIndex), cards: cards)
        }
        let unfiled = store.cards.filter { store.classInfo(id: $0.classID) == nil }.sorted { $0.dueAt < $1.dueAt }
        if !unfiled.isEmpty {
            result.append(CardGroup(id: "unfiled", title: "General", color: AvenorTheme.muted, cards: unfiled))
        }
        return result
    }

    var body: some View {
        NavigationStack {
            List {
                Section { hero }
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))

                if store.cards.isEmpty {
                    Section {
                        EmptyHint(symbol: "sparkles",
                                  title: "Your memory bank is empty",
                                  message: "Finish a focus session and write one question you can answer — or tap + to add a card. Keep each card to one idea.")
                    }
                    .listRowBackground(AvenorTheme.surface)
                }

                ForEach(groups) { group in
                    Section {
                        ForEach(group.cards) { card in
                            cardRow(card, color: group.color)
                                .contentShape(Rectangle())
                                .onTapGesture { editing = card }
                                .swipeActions {
                                    Button(role: .destructive) { store.deleteCard(id: card.id) } label: { Label("Delete", systemImage: "trash") }
                                }
                        }
                    } header: {
                        HStack {
                            Circle().fill(group.color).frame(width: 8, height: 8)
                            Text(group.title)
                            Spacer()
                            Text("\(group.cards.count)")
                        }
                    }
                    .listRowBackground(AvenorTheme.surface)
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(AvenorTheme.background.ignoresSafeArea())
            .navigationTitle("Review")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showingNew = true } label: { Image(systemName: "plus") }
                        .accessibilityLabel("Add card")
                }
            }
            .sheet(isPresented: $showingNew) { CardEditor(card: nil) }
            .sheet(item: $editing) { card in CardEditor(card: card) }
            .fullScreenCover(isPresented: $reviewing) {
                ReviewSessionView(queue: Array(store.dueCards.prefix(20)))
                    .environmentObject(store)
            }
        }
    }

    private var hero: some View {
        let due = store.dueCards.count
        return VStack(alignment: .leading, spacing: 12) {
            if due > 0 {
                Text("READY TO REVIEW")
                    .font(.caption.weight(.heavy)).tracking(1.5)
                    .foregroundStyle(.white.opacity(0.8))
                Text(due == 1 ? "1 card" : "\(due) cards")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                Text(due > 20 ? "We'll do 20 now — small sets stick better." : "About \(max(1, due / 4)) min. Answer in your head before you flip.")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.85))
                Button { reviewing = true } label: {
                    Label("Start review", systemImage: "play.fill")
                }
                .buttonStyle(PrimaryButtonStyle(tint: .white))
            } else {
                Label("All caught up", systemImage: "checkmark.seal.fill")
                    .font(.title2.bold())
                    .foregroundStyle(AvenorTheme.green)
                if let next = store.nextCardDue {
                    Text("Next card comes back \(next.formatted(.relative(presentation: .named))).")
                        .font(.subheadline).foregroundStyle(AvenorTheme.muted)
                } else {
                    Text("Add cards from your notes or after a focus session.")
                        .font(.subheadline).foregroundStyle(AvenorTheme.muted)
                }
                HStack(spacing: 10) {
                    StatTile(value: "\(store.reviewsToday)", label: "reviewed today", color: AvenorTheme.violet)
                    StatTile(value: "\(store.cards.filter { $0.box >= 4 }.count)", label: "mastered", color: AvenorTheme.green)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            if due > 0 {
                RoundedRectangle(cornerRadius: 24, style: .continuous).fill(AvenorTheme.heroGradient)
            } else {
                RoundedRectangle(cornerRadius: 24, style: .continuous).fill(AvenorTheme.surface)
            }
        }
    }

    private func cardRow(_ card: Flashcard, color: Color) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(card.front).font(.body.weight(.semibold)).lineLimit(2)
                Text(card.dueAt <= Date() ? "Due now" : "Next: \(card.dueAt.formatted(.relative(presentation: .named)))")
                    .font(.caption)
                    .foregroundStyle(card.dueAt <= Date() ? AvenorTheme.accent : AvenorTheme.muted)
            }
            Spacer()
            MasteryDots(box: card.box, color: color)
        }
        .padding(.vertical, 2)
    }
}

/// Five dots = five Leitner boxes. Visible progress toward "mastered".
struct MasteryDots: View {
    let box: Int
    let color: Color
    var body: some View {
        HStack(spacing: 3) {
            ForEach(1...5, id: \.self) { level in
                Circle()
                    .fill(level <= box ? color : AvenorTheme.elevated)
                    .frame(width: 7, height: 7)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Mastery \(box) of 5")
    }
}

// MARK: - Review session

struct ReviewSessionView: View {
    @EnvironmentObject private var store: AvenorStore
    @Environment(\.dismiss) private var dismiss
    @State private var queue: [Flashcard]
    @State private var revealed = false
    @State private var firstTryCorrect = 0
    @State private var missed: Set<UUID> = []
    @State private var pulse = 0
    @State private var total: Int

    /// The queue is captured once; later store updates don't reshuffle a session in progress.
    init(queue: [Flashcard]) {
        _queue = State(initialValue: queue)
        _total = State(initialValue: queue.count)
    }

    private var done: Int { total - queue.count }

    var body: some View {
        ZStack {
            AvenorTheme.background.ignoresSafeArea()
            if let card = queue.first {
                cardView(card)
            } else {
                summary
            }
        }
        .sensoryFeedback(.selection, trigger: pulse)
    }

    private func cardView(_ card: Flashcard) -> some View {
        let cls = store.classInfo(id: card.classID)
        return VStack(spacing: 20) {
            HStack(spacing: 12) {
                Button { dismiss() } label: {
                    Image(systemName: "xmark")
                        .font(.headline)
                        .frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                }
                .accessibilityLabel("Close review")
                ProgressView(value: Double(max(done, 0)), total: Double(max(total, 1)))
                    .tint(AvenorTheme.accent)
                Text("\(max(done, 0))/\(total)")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(AvenorTheme.muted)
            }
            Spacer()
            VStack(spacing: 18) {
                if let cls {
                    Chip(cls.name, color: AvenorTheme.classColor(cls.colorIndex))
                }
                Text(card.front)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                if revealed {
                    Divider().overlay(AvenorTheme.border)
                    Text(card.back)
                        .font(.title3)
                        .multilineTextAlignment(.center)
                        .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
            }
            .padding(28)
            .frame(maxWidth: .infinity, minHeight: 300)
            .background(AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 28, style: .continuous).stroke(AvenorTheme.border, lineWidth: 1))
            .onTapGesture { withAnimation(.snappy) { revealed = true } }
            Spacer()
            // Hick's Law: two honest choices, not a 4-button difficulty scale.
            if revealed {
                Text("Did you remember it?").font(.subheadline).foregroundStyle(AvenorTheme.muted)
                HStack(spacing: 12) {
                    Button { grade(card, remembered: false) } label: {
                        Label("Not yet", systemImage: "arrow.counterclockwise")
                    }
                    .buttonStyle(SecondaryButtonStyle())
                    Button { grade(card, remembered: true) } label: {
                        Label("Got it", systemImage: "checkmark")
                    }
                    .buttonStyle(PrimaryButtonStyle(tint: AvenorTheme.green))
                }
            } else {
                Text("Answer in your head first — the effort is what makes it stick.")
                    .font(.footnote)
                    .foregroundStyle(AvenorTheme.muted)
                    .multilineTextAlignment(.center)
                Button("Show answer") { withAnimation(.snappy) { revealed = true } }
                    .buttonStyle(PrimaryButtonStyle())
            }
        }
        .padding(24)
    }

    /// Peak-End: finish on what you *did* remember, and frame misses as normal.
    private var summary: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "trophy.fill")
                .font(.system(size: 72))
                .foregroundStyle(AvenorTheme.gold)
                .symbolEffect(.bounce, value: pulse)
            Text("Review complete!").font(.largeTitle.bold())
            Text(total == 0 ? "Nothing was due."
                 : "You remembered \(firstTryCorrect) of \(total) on the first try.")
                .font(.title3)
                .multilineTextAlignment(.center)
            if !missed.isEmpty {
                Text("The \(missed.count) you missed will come back tomorrow. Forgetting and re-learning is exactly how memories get stronger.")
                    .font(.subheadline)
                    .foregroundStyle(AvenorTheme.muted)
                    .multilineTextAlignment(.center)
            }
            HStack(spacing: 10) {
                StatTile(value: "\(store.reviewsToday)", label: "reviews today", color: AvenorTheme.violet)
                StatTile(value: "\(store.streak)", label: "day streak", color: AvenorTheme.orange)
            }
            Spacer()
            Button("Done") { dismiss() }
                .buttonStyle(PrimaryButtonStyle())
        }
        .padding(24)
    }

    private func grade(_ card: Flashcard, remembered: Bool) {
        let relearning = missed.contains(card.id)
        store.review(cardID: card.id, remembered: remembered, relearning: relearning)
        queue.removeFirst()
        if remembered {
            if !relearning { firstTryCorrect += 1 }
        } else {
            missed.insert(card.id)
            queue.append(card)   // see it again at the end of this set
        }
        revealed = false
        pulse += 1
    }
}

// MARK: - Card editor

struct CardEditor: View {
    @EnvironmentObject private var store: AvenorStore
    @Environment(\.dismiss) private var dismiss
    private let original: Flashcard?
    @State private var front: String
    @State private var back: String
    @State private var classID: UUID?
    @State private var error = ""

    init(card: Flashcard?) {
        original = card
        _front = State(initialValue: card?.front ?? "")
        _back = State(initialValue: card?.back ?? "")
        _classID = State(initialValue: card?.classID)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Question", text: $front, axis: .vertical)
                    TextField("Answer", text: $back, axis: .vertical)
                } footer: {
                    Text("One idea per card. Short cards are faster to review and easier to remember.")
                }
                Section {
                    Picker("Class", selection: $classID) {
                        Text("None").tag(UUID?.none)
                        ForEach(store.classes) { cls in Text(cls.name).tag(Optional(cls.id)) }
                    }
                }
                if let original {
                    Section {
                        LabeledContent("Mastery") { MasteryDots(box: original.box, color: store.color(forClassID: original.classID)) }
                        Button("Delete card", role: .destructive) {
                            store.deleteCard(id: original.id)
                            dismiss()
                        }
                    }
                }
                if !error.isEmpty { Text(error).foregroundStyle(AvenorTheme.red) }
            }
            .navigationTitle(original == nil ? "New Card" : "Edit Card")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { save() }.fontWeight(.semibold) }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() {
        let q = front.trimmingCharacters(in: .whitespacesAndNewlines)
        let a = back.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty, !a.isEmpty else { error = "Add both a question and an answer."; return }
        if var card = original {
            card.front = q
            card.back = a
            card.classID = classID
            store.updateCard(card)
        } else {
            store.addCard(Flashcard(classID: classID, front: q, back: a))
        }
        dismiss()
    }
}

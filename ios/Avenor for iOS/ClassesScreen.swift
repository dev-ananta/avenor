import SwiftUI
import Foundation

/// Classes give everything else its color. Each class gets a consistent color across tasks,
/// events and review cards, so students can recognize a subject at a glance instead of reading.
struct ClassesView: View {
    @EnvironmentObject private var store: AvenorStore
    @State private var showingNew = false
    @State private var editing: AvenorClass?
    @State private var pendingDelete: AvenorClass?

    var body: some View {
        List {
            if store.classes.isEmpty {
                Section {
                    EmptyHint(symbol: "books.vertical.fill",
                              title: "No classes yet",
                              message: "Add your classes so tasks, tests and review cards get color-coded automatically.")
                }
                .listRowBackground(AvenorTheme.surface)
            }
            Section {
                ForEach(store.classes) { cls in
                    classRow(cls)
                        .contentShape(Rectangle())
                        .onTapGesture { editing = cls }
                        .swipeActions {
                            Button(role: .destructive) { pendingDelete = cls } label: { Label("Delete", systemImage: "trash") }
                        }
                }
            }
            .listRowBackground(AvenorTheme.surface)
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showingNew = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Add class")
            }
        }
        .sheet(isPresented: $showingNew) { ClassEditor(item: nil) }
        .sheet(item: $editing) { cls in ClassEditor(item: cls) }
        .confirmationDialog("Delete this class?",
                            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
                            titleVisibility: .visible,
                            presenting: pendingDelete) { cls in
            Button("Delete \(cls.name)", role: .destructive) { store.deleteClass(id: cls.id) }
        } message: { _ in
            Text("Its tasks and review cards are kept — they just won't be filed under a class.")
        }
    }

    private func classRow(_ cls: AvenorClass) -> some View {
        let color = AvenorTheme.classColor(cls.colorIndex)
        let openTasks = store.tasks.filter { $0.classID == cls.id && !$0.completed }.count
        let cardCount = store.cards.filter { $0.classID == cls.id }.count
        return HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(color.opacity(0.18))
                .frame(width: 46, height: 46)
                .overlay(Image(systemName: "book.closed.fill").foregroundStyle(color))
            VStack(alignment: .leading, spacing: 4) {
                Text(cls.name).font(.headline)
                let detail = [cls.teacher, cls.room.isEmpty ? "" : "Room \(cls.room)"].filter { !$0.isEmpty }.joined(separator: " · ")
                if !detail.isEmpty { Text(detail).font(.subheadline).foregroundStyle(AvenorTheme.muted) }
                Text("\(openTasks) open \(openTasks == 1 ? "task" : "tasks") · \(cardCount) \(cardCount == 1 ? "card" : "cards")")
                    .font(.caption).foregroundStyle(AvenorTheme.muted)
            }
            Spacer()
        }
        .padding(.vertical, 2)
    }
}

struct ClassEditor: View {
    @EnvironmentObject private var store: AvenorStore
    @Environment(\.dismiss) private var dismiss
    private let original: AvenorClass?
    @State private var name: String
    @State private var teacher: String
    @State private var room: String
    @State private var colorIndex: Int
    @State private var error = ""

    init(item: AvenorClass?) {
        original = item
        _name = State(initialValue: item?.name ?? "")
        _teacher = State(initialValue: item?.teacher ?? "")
        _room = State(initialValue: item?.room ?? "")
        _colorIndex = State(initialValue: item?.colorIndex ?? 0)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Class name (e.g. Biology)", text: $name)
                    TextField("Teacher", text: $teacher)
                    TextField("Room", text: $room)
                }
                Section("Color") {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 12) {
                        ForEach(AvenorTheme.classPalette.indices, id: \.self) { index in
                            Button { colorIndex = index } label: {
                                Circle()
                                    .fill(AvenorTheme.classPalette[index])
                                    .frame(width: 36, height: 36)
                                    .overlay(Circle().stroke(Color.white, lineWidth: colorIndex == index ? 3 : 0).padding(-4))
                                    .frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Color \(index + 1)")
                            .accessibilityAddTraits(colorIndex == index ? .isSelected : [])
                        }
                    }
                    .padding(.vertical, 6)
                }
                if !error.isEmpty { Text(error).foregroundStyle(AvenorTheme.red) }
            }
            .navigationTitle(original == nil ? "New Class" : "Edit Class")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { save() }.fontWeight(.semibold) }
            }
            .onAppear {
                if original == nil { colorIndex = store.classes.count % AvenorTheme.classPalette.count }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { error = "Enter a class name."; return }
        if var updated = original {
            updated.name = clean
            updated.teacher = teacher
            updated.room = room
            updated.colorIndex = colorIndex
            store.updateClass(updated)
        } else {
            store.addClass(AvenorClass(name: clean, teacher: teacher, room: room, colorIndex: colorIndex))
        }
        dismiss()
    }
}

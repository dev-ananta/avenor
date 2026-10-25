import SwiftUI
import Foundation

// MARK: - Quick add (Tesler's Law + Fitts's Law)
// One text field, pinned in the thumb zone, that understands "Bio essay fri 45m !".
// The app does the parsing so the student never has to fill in a form.

struct QuickAddBar: View {
    @EnvironmentObject private var store: AvenorStore
    @State private var text = ""
    @State private var showingForm = false
    @State private var addedCount = 0
    @FocusState private var focused: Bool

    private var parsed: QuickAddResult { QuickAddParser.parse(text, classes: store.classes) }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if focused && !text.trimmingCharacters(in: .whitespaces).isEmpty {
                preview.transition(.move(edge: .bottom).combined(with: .opacity))
            }
            HStack(spacing: 10) {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
                    .foregroundStyle(AvenorTheme.accent)
                TextField("Add a task — try “Bio essay fri 45m !”", text: $text)
                    .focused($focused)
                    .submitLabel(.done)
                    .onSubmit(add)
                    .textInputAutocapitalization(.sentences)
                if text.isEmpty {
                    Button { showingForm = true } label: {
                        Image(systemName: "slider.horizontal.3")
                            .frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                    }
                    .accessibilityLabel("Add task with details")
                } else {
                    Button(action: add) {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.title)
                            .frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                    }
                    .accessibilityLabel("Add task")
                }
            }
            .padding(.leading, 14)
            .padding(.trailing, 4)
            .frame(minHeight: 54)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(AvenorTheme.border, lineWidth: 1))
        }
        .animation(.easeOut(duration: 0.2), value: focused)
        .sheet(isPresented: $showingForm) { TaskEditor(task: nil) }
        .sensoryFeedback(.success, trigger: addedCount)
    }

    private var preview: some View {
        let p = parsed
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                Chip(p.title.isEmpty ? "Add a title" : p.title, color: .white)
                if let due = p.dueDate { Chip(Planner.dueLabel(due), color: AvenorTheme.accent, systemImage: "calendar") }
                if let minutes = p.minutes { Chip("\(minutes) min", color: AvenorTheme.sky, systemImage: "clock") }
                if let priority = p.priority { Chip(priority.rawValue, color: AvenorTheme.priorityColor(priority), systemImage: "flag.fill") }
                if let name = p.className { Chip(name, color: store.color(forClassID: p.classID), systemImage: "book.closed.fill") }
            }
            .padding(.horizontal, 4)
        }
    }

    private func add() {
        let p = parsed
        let title = p.title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else { return }
        store.addTask(AvenorTask(
            title: title,
            className: p.className ?? "",
            classID: p.classID,
            dueDate: p.dueDate,
            priority: p.priority ?? .medium,
            estimatedMinutes: p.minutes ?? store.profile.defaultSessionMinutes
        ))
        text = ""
        addedCount += 1
    }
}

// MARK: - Task row

struct TaskRowView: View {
    @EnvironmentObject private var store: AvenorStore
    let task: AvenorTask
    let onToggle: () -> Void

    private var isOverdue: Bool {
        guard let due = task.dueDate, !task.completed else { return false }
        return Planner.bucket(for: task) == .overdue && due < Date()
    }

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            Button(action: onToggle) {
                Image(systemName: task.completed ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(task.completed ? AvenorTheme.green : AvenorTheme.muted)
                    .symbolEffect(.bounce, value: task.completed)
                    .frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(task.completed ? "Mark \(task.title) incomplete" : "Mark \(task.title) complete")

            VStack(alignment: .leading, spacing: 5) {
                Text(task.title)
                    .font(.body.weight(.semibold))
                    .strikethrough(task.completed)
                    .foregroundStyle(task.completed ? AvenorTheme.muted : Color.white)
                    .lineLimit(2)
                HStack(spacing: 6) {
                    let name = store.className(for: task)
                    if !name.isEmpty { Chip(name, color: store.color(forClassID: task.classID)) }
                    if let due = task.dueDate, !task.completed {
                        Chip(Planner.dueLabel(due), color: isOverdue ? AvenorTheme.red : AvenorTheme.muted, systemImage: "calendar")
                    }
                    if !task.completed {
                        Chip("\(task.estimatedMinutes)m", systemImage: "clock")
                    }
                }
            }
            Spacer(minLength: 0)
            if task.priority == .high && !task.completed {
                Image(systemName: "exclamationmark.circle.fill")
                    .foregroundStyle(AvenorTheme.red)
                    .accessibilityLabel("High priority")
            }
        }
        .padding(.vertical, 2)
    }
}

// MARK: - Task editor (create + edit share one form)

struct TaskEditor: View {
    @EnvironmentObject private var store: AvenorStore
    @Environment(\.dismiss) private var dismiss
    private let original: AvenorTask?
    @State private var draft: AvenorTask
    @State private var hasDueDate: Bool
    @State private var error = ""

    init(task: AvenorTask?) {
        original = task
        let base = task ?? AvenorTask(title: "")
        _draft = State(initialValue: base)
        _hasDueDate = State(initialValue: base.dueDate != nil)
    }

    private var dueBinding: Binding<Date> {
        Binding(
            get: { draft.dueDate ?? Calendar.current.startOfDay(for: Date()) },
            set: { draft.dueDate = $0 }
        )
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("What do you need to do?", text: $draft.title)
                    TextField("Notes (optional)", text: $draft.details, axis: .vertical)
                }
                Section {
                    Picker("Class", selection: $draft.classID) {
                        Text("None").tag(UUID?.none)
                        ForEach(store.classes) { cls in
                            Text(cls.name).tag(Optional(cls.id))
                        }
                    }
                    Picker("Priority", selection: $draft.priority) {
                        ForEach(TaskPriority.allCases) { Text($0.rawValue).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    Stepper(value: $draft.estimatedMinutes, in: 5...600, step: 5) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("About \(draft.estimatedMinutes) min")
                            let count = Planner.sessions(forMinutes: draft.estimatedMinutes, sessionLength: store.profile.defaultSessionMinutes)
                            Text(count == 1 ? "1 focus session" : "\(count) focus sessions")
                                .font(.caption).foregroundStyle(AvenorTheme.muted)
                        }
                    }
                    Toggle("Due date", isOn: $hasDueDate.animation())
                    if hasDueDate {
                        DatePicker("Due", selection: dueBinding, displayedComponents: .date)
                    }
                }
                if !error.isEmpty {
                    Text(error).foregroundStyle(AvenorTheme.red)
                }
                if original != nil {
                    Section {
                        Button("Delete task", role: .destructive) {
                            store.deleteTask(id: draft.id)
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle(original == nil ? "New Task" : "Edit Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { save() }.fontWeight(.semibold) }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() {
        var task = draft
        task.title = task.title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !task.title.isEmpty else { error = "Give the task a title."; return }
        task.dueDate = hasDueDate ? (task.dueDate ?? Calendar.current.startOfDay(for: Date())) : nil
        if let cls = store.classInfo(id: task.classID) {
            task.className = cls.name
        } else if original?.classID != nil {
            task.className = ""
        }
        if original == nil { store.addTask(task) } else { store.updateTask(task) }
        dismiss()
    }
}

import SwiftUI
import Foundation

/// Tasks, Calendar and Classes live under one tab (Hick's Law: fewer top-level choices),
/// switched with a standard segmented control (Jakob's Law).
enum PlanSegment: String, CaseIterable, Identifiable {
    case tasks = "Tasks"
    case calendar = "Calendar"
    case classes = "Classes"
    var id: String { rawValue }
}

struct PlanScreen: View {
    @State private var segment: PlanSegment = .tasks

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Section", selection: $segment) {
                    ForEach(PlanSegment.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 20)
                .padding(.vertical, 8)

                switch segment {
                case .tasks: TaskListView()
                case .calendar: CalendarView()
                case .classes: ClassesView()
                }
            }
            .background(AvenorTheme.background.ignoresSafeArea())
            .navigationTitle("Plan")
        }
    }
}

// MARK: - Tasks

private struct TaskGroup: Identifiable {
    let bucket: TaskBucket
    let items: [AvenorTask]
    var id: TaskBucket { bucket }
}

struct TaskListView: View {
    @EnvironmentObject private var store: AvenorStore
    @EnvironmentObject private var focus: FocusEngine
    @State private var editing: AvenorTask?
    @State private var showCompleted = false
    @State private var pulse = 0

    /// Miller's Law: chunk one long list into small, meaningful groups.
    private var groups: [TaskGroup] {
        let active = store.rankedActive
        return TaskBucket.allCases.compactMap { bucket in
            let items = active.filter { Planner.bucket(for: $0) == bucket }
            return items.isEmpty ? nil : TaskGroup(bucket: bucket, items: items)
        }
    }

    private var completed: [AvenorTask] {
        store.tasks.filter(\.completed).sorted { ($0.completedAt ?? .distantPast) > ($1.completedAt ?? .distantPast) }
    }

    var body: some View {
        List {
            if groups.isEmpty && completed.isEmpty {
                Section {
                    EmptyHint(symbol: "checklist",
                              title: "No tasks yet",
                              message: "Type one below. Add a day, a time and “!” for important — e.g. “Chem worksheet tmrw 30m !”.")
                }
                .listRowBackground(AvenorTheme.surface)
            }
            ForEach(groups) { group in
                Section {
                    ForEach(group.items) { task in row(task) }
                } header: {
                    HStack {
                        Text(group.bucket.rawValue)
                        Spacer()
                        Text("\(group.items.count)")
                    }
                    .foregroundStyle(group.bucket == .overdue ? AvenorTheme.red : AvenorTheme.muted)
                }
            }
            if !completed.isEmpty {
                Section {
                    DisclosureGroup(isExpanded: $showCompleted) {
                        ForEach(completed.prefix(50)) { task in row(task) }
                    } label: {
                        Text("Completed (\(completed.count))").foregroundStyle(AvenorTheme.muted)
                    }
                    .listRowBackground(AvenorTheme.surface)
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 8) {
                if store.recentlyDeleted != nil { undoToast.transition(.move(edge: .bottom).combined(with: .opacity)) }
                QuickAddBar()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
            .animation(.snappy, value: store.recentlyDeleted?.id)
        }
        .sheet(item: $editing) { task in TaskEditor(task: task) }
        .sensoryFeedback(.success, trigger: pulse)
    }

    private func row(_ task: AvenorTask) -> some View {
        TaskRowView(task: task) { toggle(task) }
            .contentShape(Rectangle())
            .onTapGesture { editing = task }
            .listRowBackground(AvenorTheme.surface)
            .swipeActions(edge: .leading, allowsFullSwipe: true) {
                Button { toggle(task) } label: {
                    Label(task.completed ? "Undo" : "Done", systemImage: task.completed ? "arrow.uturn.backward" : "checkmark")
                }
                .tint(AvenorTheme.green)
            }
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                Button(role: .destructive) {
                    withAnimation { store.deleteTask(id: task.id) }
                } label: { Label("Delete", systemImage: "trash") }
                if !task.completed {
                    Button {
                        focus.start(taskID: task.id, label: task.title, minutes: store.profile.defaultSessionMinutes)
                    } label: { Label("Focus", systemImage: "timer") }
                    .tint(AvenorTheme.accent)
                }
            }
            .contextMenu {
                if !task.completed {
                    Button { focus.start(taskID: task.id, label: task.title, minutes: store.profile.defaultSessionMinutes) } label: {
                        Label("Start focus session", systemImage: "timer")
                    }
                }
                Button { editing = task } label: { Label("Edit", systemImage: "pencil") }
                Button(role: .destructive) { store.deleteTask(id: task.id) } label: { Label("Delete", systemImage: "trash") }
            }
    }

    private var undoToast: some View {
        HStack {
            Text("Task deleted").font(.subheadline)
            Spacer()
            Button("Undo") { withAnimation { store.undoDelete() } }
                .fontWeight(.bold)
                .frame(minWidth: AvenorTheme.minTap, minHeight: AvenorTheme.minTap)
        }
        .padding(.horizontal, 16)
        .background(AvenorTheme.elevated, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .task(id: store.recentlyDeleted?.id) {
            do {
                try await Task.sleep(for: .seconds(5))
                store.recentlyDeleted = nil
            } catch {}
        }
    }

    private func toggle(_ task: AvenorTask) {
        let finishing = !task.completed
        withAnimation(.snappy) { store.toggle(task) }
        if finishing { pulse += 1 }
    }
}

// MARK: - Calendar

struct CalendarView: View {
    @EnvironmentObject private var store: AvenorStore
    @State private var selectedDay = Calendar.current.startOfDay(for: Date())
    @State private var weekOffset = 0
    @State private var showingNewEvent = false
    @State private var editingEvent: AvenorEvent?
    @State private var editingTask: AvenorTask?

    private var calendar: Calendar { Calendar.current }

    private var weekDays: [Date] {
        let today = calendar.startOfDay(for: Date())
        let start = calendar.dateInterval(of: .weekOfYear, for: today)?.start ?? today
        let shifted = calendar.date(byAdding: .day, value: weekOffset * 7, to: start) ?? start
        return (0..<7).compactMap { calendar.date(byAdding: .day, value: $0, to: shifted) }
    }

    private var dayEvents: [AvenorEvent] { store.schedule(on: selectedDay) }
    private var dayTasks: [AvenorTask] { store.tasksDue(on: selectedDay) }

    var body: some View {
        List {
            Section {
                weekStrip
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))

            Section {
                if dayEvents.isEmpty && dayTasks.isEmpty {
                    Text("Nothing scheduled. Tap + to add a class, test, or practice.")
                        .font(.subheadline)
                        .foregroundStyle(AvenorTheme.muted)
                }
                ForEach(dayEvents) { event in
                    eventRow(event)
                        .contentShape(Rectangle())
                        .onTapGesture { editingEvent = event }
                        .swipeActions {
                            Button(role: .destructive) { store.deleteEvent(id: event.id) } label: { Label("Delete", systemImage: "trash") }
                        }
                }
                ForEach(dayTasks) { task in
                    TaskRowView(task: task) { store.toggle(task) }
                        .contentShape(Rectangle())
                        .onTapGesture { editingTask = task }
                }
            } header: {
                Text(selectedDay.formatted(.dateTime.weekday(.wide).month(.wide).day()))
            }
            .listRowBackground(AvenorTheme.surface)
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showingNewEvent = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Add event")
            }
        }
        .sheet(isPresented: $showingNewEvent) { EventEditor(event: nil, day: selectedDay) }
        .sheet(item: $editingEvent) { event in EventEditor(event: event, day: event.date) }
        .sheet(item: $editingTask) { task in TaskEditor(task: task) }
    }

    private var weekStrip: some View {
        VStack(spacing: 10) {
            HStack {
                Text((weekDays.first ?? Date()).formatted(.dateTime.month(.wide).year()))
                    .font(.headline)
                Spacer()
                Button { weekOffset -= 1 } label: {
                    Image(systemName: "chevron.left").frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                }
                .accessibilityLabel("Previous week")
                Button {
                    weekOffset = 0
                    selectedDay = calendar.startOfDay(for: Date())
                } label: { Text("Today").font(.subheadline.weight(.semibold)).frame(minHeight: AvenorTheme.minTap) }
                Button { weekOffset += 1 } label: {
                    Image(systemName: "chevron.right").frame(width: AvenorTheme.minTap, height: AvenorTheme.minTap)
                }
                .accessibilityLabel("Next week")
            }
            .padding(.horizontal, 4)
            HStack(spacing: 6) {
                ForEach(weekDays, id: \.self) { day in dayCell(day) }
            }
        }
        .padding(.vertical, 6)
    }

    private func dayCell(_ day: Date) -> some View {
        let isSelected = calendar.isDate(day, inSameDayAs: selectedDay)
        let isToday = calendar.isDateInToday(day)
        let hasItems = !store.schedule(on: day).isEmpty || !store.tasksDue(on: day).filter { !$0.completed }.isEmpty
        return Button {
            withAnimation(.snappy) { selectedDay = day }
        } label: {
            VStack(spacing: 4) {
                Text(day.formatted(.dateTime.weekday(.narrow)))
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(isSelected ? Color.black.opacity(0.7) : AvenorTheme.muted)
                Text(day.formatted(.dateTime.day()))
                    .font(.headline)
                    .foregroundStyle(isSelected ? Color.black : (isToday ? AvenorTheme.accent : Color.white))
                Circle()
                    .fill(hasItems ? (isSelected ? Color.black : AvenorTheme.accent) : Color.clear)
                    .frame(width: 5, height: 5)
            }
            .frame(maxWidth: .infinity, minHeight: 60)
            .background(isSelected ? AvenorTheme.accent : AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(day.formatted(date: .complete, time: .omitted))
    }

    private func eventRow(_ event: AvenorEvent) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(store.color(forClassID: event.classID))
                .frame(width: 4, height: 38)
            VStack(alignment: .leading, spacing: 3) {
                Text(event.title).font(.headline)
                Text(event.timeLabel ?? "All day").font(.subheadline).foregroundStyle(AvenorTheme.muted)
            }
            Spacer()
        }
        .padding(.vertical, 2)
    }
}

struct EventEditor: View {
    @EnvironmentObject private var store: AvenorStore
    @Environment(\.dismiss) private var dismiss
    private let original: AvenorEvent?
    @State private var title: String
    @State private var start: Date
    @State private var hasTime: Bool
    @State private var duration: Int
    @State private var classID: UUID?
    @State private var error = ""

    init(event: AvenorEvent?, day: Date) {
        original = event
        let calendar = Calendar.current
        let nextHour = (calendar.component(.hour, from: Date()) + 1) % 24
        let defaultStart = calendar.date(bySettingHour: nextHour, minute: 0, second: 0, of: day) ?? day
        _title = State(initialValue: event?.title ?? "")
        _start = State(initialValue: event?.date ?? defaultStart)
        _hasTime = State(initialValue: event?.hasTime ?? true)
        _duration = State(initialValue: event?.durationMinutes ?? 60)
        _classID = State(initialValue: event?.classID)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Title (e.g. Bio test, practice)", text: $title)
                    Picker("Class", selection: $classID) {
                        Text("None").tag(UUID?.none)
                        ForEach(store.classes) { cls in Text(cls.name).tag(Optional(cls.id)) }
                    }
                }
                Section {
                    Toggle("Specific time", isOn: $hasTime.animation())
                    if hasTime {
                        DatePicker("Starts", selection: $start, displayedComponents: [.date, .hourAndMinute])
                        Picker("Length", selection: $duration) {
                            ForEach([15, 30, 45, 60, 90, 120, 180], id: \.self) { minutes in
                                Text(minutes < 60 ? "\(minutes) min" : (minutes % 60 == 0 ? "\(minutes / 60) hr" : "\(minutes / 60) hr \(minutes % 60) min"))
                                    .tag(minutes)
                            }
                        }
                    } else {
                        DatePicker("Date", selection: $start, displayedComponents: .date)
                    }
                }
                if let original, !original.hasTime, let legacy = original.timeLabel {
                    Section { Text("Saved time: \(legacy)").foregroundStyle(AvenorTheme.muted) }
                }
                if !error.isEmpty { Text(error).foregroundStyle(AvenorTheme.red) }
                if original != nil {
                    Section {
                        Button("Delete event", role: .destructive) {
                            if let original { store.deleteEvent(id: original.id) }
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle(original == nil ? "New Event" : "Edit Event")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { save() }.fontWeight(.semibold) }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() {
        let clean = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { error = "Give the event a title."; return }
        var event = original ?? AvenorEvent(title: clean)
        event.title = clean
        event.hasTime = hasTime
        event.date = hasTime ? start : Calendar.current.startOfDay(for: start)
        event.durationMinutes = duration
        event.classID = classID
        if hasTime { event.startTime = ""; event.endTime = "" }
        if original == nil { store.addEvent(event) } else { store.updateEvent(event) }
        dismiss()
    }
}

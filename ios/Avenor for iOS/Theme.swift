import SwiftUI
import Foundation

// Aesthetic-Usability Effect: one calm, consistent visual system makes the app *feel* easier,
// which makes students more forgiving and more likely to keep opening it.
enum AvenorTheme {
    static let background = Color(red: 8/255, green: 9/255, blue: 11/255)
    static let surface = Color(red: 17/255, green: 19/255, blue: 24/255)
    static let elevated = Color(red: 24/255, green: 27/255, blue: 33/255)
    static let border = Color(red: 41/255, green: 45/255, blue: 53/255)
    static let muted = Color(red: 150/255, green: 155/255, blue: 166/255)
    static let accent = Color(red: 139/255, green: 156/255, blue: 255/255)
    static let green = Color(red: 101/255, green: 214/255, blue: 160/255)
    static let red = Color(red: 255/255, green: 115/255, blue: 115/255)
    static let orange = Color(red: 255/255, green: 184/255, blue: 107/255)
    static let pink = Color(red: 250/255, green: 140/255, blue: 190/255)
    static let sky = Color(red: 115/255, green: 205/255, blue: 250/255)
    static let violet = Color(red: 200/255, green: 160/255, blue: 255/255)
    static let gold = Color(red: 255/255, green: 214/255, blue: 102/255)

    static let classPalette: [Color] = [accent, green, orange, pink, sky, violet, gold, red]

    static func classColor(_ index: Int) -> Color {
        let count = classPalette.count
        return classPalette[((index % count) + count) % count]
    }

    static func priorityColor(_ priority: TaskPriority) -> Color {
        switch priority {
        case .high: return red
        case .medium: return orange
        case .low: return green
        }
    }

    /// Von Restorff Effect: only the single most important element on a screen gets this.
    static let heroGradient = LinearGradient(
        colors: [Color(red: 92/255, green: 84/255, blue: 240/255), Color(red: 139/255, green: 156/255, blue: 255/255)],
        startPoint: .topLeading, endPoint: .bottomTrailing
    )

    /// Fitts's Law: no tap target smaller than Apple's 44 pt minimum; primary actions are bigger.
    static let minTap: CGFloat = 44
    static let primaryHeight: CGFloat = 56
}

extension AvenorStore {
    func color(forClassID id: UUID?) -> Color {
        guard let cls = classInfo(id: id) else { return AvenorTheme.muted }
        return AvenorTheme.classColor(cls.colorIndex)
    }
}

// MARK: - Building blocks

struct Panel<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        content.padding(16).frame(maxWidth: .infinity, alignment: .leading)
            .background(AvenorTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(AvenorTheme.border, lineWidth: 1))
    }
}

struct ScreenTitle: View {
    let title: String
    var subtitle: String? = nil
    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title).font(.system(size: 30, weight: .bold, design: .rounded))
            if let subtitle { Text(subtitle).foregroundStyle(AvenorTheme.muted).font(.subheadline) }
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct SectionHeading: View {
    let title: String
    var trailing: String? = nil
    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title).font(.title3.bold())
            Spacer()
            if let trailing { Text(trailing).font(.subheadline).foregroundStyle(AvenorTheme.muted) }
        }
    }
}

struct Chip: View {
    let text: String
    var color: Color = AvenorTheme.muted
    var systemImage: String? = nil

    init(_ text: String, color: Color = AvenorTheme.muted, systemImage: String? = nil) {
        self.text = text
        self.color = color
        self.systemImage = systemImage
    }

    var body: some View {
        HStack(spacing: 4) {
            if let systemImage { Image(systemName: systemImage) }
            Text(text).lineLimit(1)
        }
        .font(.caption.weight(.semibold))
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .foregroundStyle(color)
        .background(color.opacity(0.14), in: Capsule())
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var tint: Color = AvenorTheme.accent
    var foreground: Color = .black
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: AvenorTheme.primaryHeight)
            .foregroundStyle(foreground)
            .background(tint, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 50)
            .foregroundStyle(.white)
            .background(AvenorTheme.elevated, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(AvenorTheme.border, lineWidth: 1))
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

struct ProgressRing: View {
    var progress: Double
    var lineWidth: CGFloat = 10
    var tint: Color = AvenorTheme.accent

    var body: some View {
        ZStack {
            Circle().stroke(AvenorTheme.elevated, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: min(max(progress, 0), 1))
                .stroke(tint, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.6), value: progress)
        }
    }
}

struct StatTile: View {
    let value: String
    let label: String
    var color: Color = .white
    var body: some View {
        VStack(spacing: 4) {
            Text(value).font(.title2.bold()).foregroundStyle(color).contentTransition(.numericText())
            Text(label).font(.caption).foregroundStyle(AvenorTheme.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(AvenorTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// A short, friendly empty state that always tells the student what to do next.
struct EmptyHint: View {
    let symbol: String
    let title: String
    let message: String
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: symbol).font(.title2).foregroundStyle(AvenorTheme.accent)
            Text(title).font(.headline)
            Text(message).font(.subheadline).foregroundStyle(AvenorTheme.muted)
        }
        .padding(.vertical, 6)
    }
}

enum Clock {
    static func string(_ seconds: TimeInterval) -> String {
        let total = Int(seconds.rounded(.up))
        return String(format: "%02d:%02d", total / 60, total % 60)
    }
}

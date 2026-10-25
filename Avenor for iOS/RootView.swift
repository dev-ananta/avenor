import SwiftUI
import Foundation

/// Hick's Law: four destinations instead of five, each with one clear job.
/// Jakob's Law: a standard iOS tab bar, SF Symbols, sheets and swipe actions — nothing to re-learn.
enum Destination: String, CaseIterable, Hashable {
    case today = "Today"
    case plan = "Plan"
    case focus = "Focus"
    case review = "Review"

    var symbol: String {
        switch self {
        case .today: return "sun.max.fill"
        case .plan: return "checklist"
        case .focus: return "timer"
        case .review: return "rectangle.stack.fill"
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var store: AvenorStore
    @EnvironmentObject private var focus: FocusEngine
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if store.profile.onboarded {
                TabView(selection: $store.tab) {
                    TodayScreen()
                        .tabItem { Label(Destination.today.rawValue, systemImage: Destination.today.symbol) }
                        .tag(Destination.today)
                    PlanScreen()
                        .tabItem { Label(Destination.plan.rawValue, systemImage: Destination.plan.symbol) }
                        .tag(Destination.plan)
                    FocusScreen()
                        .tabItem { Label(Destination.focus.rawValue, systemImage: Destination.focus.symbol) }
                        .tag(Destination.focus)
                    ReviewScreen()
                        .tabItem { Label(Destination.review.rawValue, systemImage: Destination.review.symbol) }
                        .tag(Destination.review)
                        .badge(store.dueCards.count)
                }
                .tint(AvenorTheme.accent)
            } else {
                OnboardingView()
            }
        }
        .background(AvenorTheme.background.ignoresSafeArea())
        .fullScreenCover(isPresented: $focus.isPresented) {
            FocusSessionView()
                .environmentObject(store)
                .environmentObject(focus)
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .background {
                focus.sceneChanged(toBackground: true, nudge: store.profile.nudgeWhenLeaving)
            } else if newPhase == .active {
                focus.sceneChanged(toBackground: false, nudge: false)
            }
        }
    }
}

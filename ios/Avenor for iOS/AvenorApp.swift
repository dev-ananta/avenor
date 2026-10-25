import SwiftUI
import StoreKit

@main
struct AvenorApp: App {
    @StateObject private var store = AvenorStore()
    @StateObject private var focus = FocusEngine()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(focus)
                .preferredColorScheme(.dark)
                .task { await TipJar.listenForTransactions() }
        }
    }
}

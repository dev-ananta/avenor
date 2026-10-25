# Avenor for iOS — version 03

A free, private, on-device study app for students in 8th grade through college:
**plan → focus → remember**, all in one place.

| Tab | What it does |
|---|---|
| **Today** | One recommended task ("Focus Now") with a one-tap Start, a daily focus ring, streak, up to 3 next tasks, today's events, cards due, and an evening wrap-up. |
| **Plan** | Tasks (auto-grouped Overdue / Today / Tomorrow / This week / Later), a week calendar, and color-coded classes. Natural-language quick add. Swipe to finish, delete (with Undo), or focus. |
| **Focus** | Study Mode: pick a task + 15/25/45 min, then a distraction-free full-screen timer → 30-second recall ("write one question you can now answer") → guided break → next round. |
| **Review** | Spaced-repetition flashcards (5-box Leitner: 1, 2, 4, 8, 16 days). Cards are created from your own focus-session recall or added by hand. |
| **You** (avatar on Today) | Name, grade, daily goal, session length, reminders, stats, optional tip jar, reset. |

## New in this update (synced with Android and the web)
- **"Just 5 min"** starter on Today and Focus. Survey: 39% delay or distract themselves when stressed.
- **60-second breathing reset** on every break. About 1 in 4 asked for something calming.
- **"Need to talk to someone?"** section in You with 988 call/text links. Research: AI chatbots can miss signs of crisis, so Avenor points to people.
- Typing "bio" now auto-tags a class named "Biology", the same rule as Android and the web.

## Open & build

1. Open `Avenor for iOS.xcodeproj` in Xcode (the project uses folder-synchronized groups, so every `.swift` file in `Avenor for iOS/` is already in the target).
2. Pick an iPhone simulator and press **⌘R**. Deployment target is iOS 27; no third-party packages.
3. To test the tip jar: create three **consumable** in-app purchases in App Store Connect with the IDs in `TipJar.productIDs` (`YouScreen.swift`), or add a local StoreKit configuration file for the simulator. Until then the tip rows show as unavailable — nothing else depends on them.

### Project fixes made in this version
- Removed a `SWIFT_OBJC_BRIDGING_HEADER` setting that pointed at a non-existent `MyApp/` path (would fail the build) and the empty bridging header.
- Limited the target to iPhone + iPad (`TARGETED_DEVICE_FAMILY = 1,2`, iOS platforms only) so APIs like haptic feedback don't break a visionOS/macOS build.

### Data compatibility
All models decode with defaults for missing fields, so tasks, classes and events saved by Avenor 1.0 load unchanged. Existing users skip onboarding automatically.

### Verification status
The source was written and reviewed against the iOS 17+ SwiftUI APIs, but this package **has not yet been compiled in Xcode** (no Xcode/iOS SDK was available where it was written). The quick-add parser and streak rules were checked with a logic mirror. Build once in Xcode and fix any diagnostics before submitting.

## Where the psychology lives in the code
See `DESIGN.md` for the full law-by-law mapping. The short version:

| Principle | Where |
|---|---|
| Jakob's Law | Standard tab bar, segmented controls, swipe actions, sheets, SF Symbols (`RootView`, `PlanScreen`) |
| Fitts's Law | 56 pt primary buttons pinned in the thumb zone, 44 pt minimum targets, hold-to-quit (`Theme`, `FocusScreen`) |
| Hick's Law | 4 tabs instead of 5; one "Focus Now" pick; 3 session lengths; 2 review answers (`TodayScreen`, `ReviewScreen`) |
| Miller's Law | Up to 3 next tasks, 5 focus candidates, 20-card review sets, bucketed task lists, work chunked into sessions (`Engine.swift`) |
| Tesler's Law | Natural-language quick add, auto-ranking, default reminder times, auto class colors (`Engine.swift`, `Reminders.swift`) |
| Aesthetic-Usability | One calm dark theme, class colors, motion and haptics (`Theme.swift`) |
| Peak-End Rule | Session-complete screen, review summary, evening "Today's wrap", no-shame early exits |
| Von Restorff | The gradient Focus Now / Review hero card is the only bright block on each screen |

## Cost to run
No servers, accounts, analytics or third-party SDKs: everything is stored and scheduled on the phone. The only fixed cost is the Apple Developer Program membership (a fee waiver is available to qualifying schools and nonprofits).

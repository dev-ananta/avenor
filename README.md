# Avenor 3.0

**Plan it. Focus. Remember it.** A free, private study app for students in 8th grade through college, on iPhone, Android and the web.
Built for the 2026 Congressional App Challenge.

| Branch / file | What's inside | Build with |
|---|---|---|
| `ios` branch | SwiftUI app (iOS 27+) | Xcode: open `Avenor for iOS.xcodeproj` and press ⌘R |
| `android` branch | Kotlin + Jetpack Compose app (Android 8+) | Android Studio: open the project, Sync, then Run. `./gradlew test` runs the engine tests. |
| `site` branch | Landing page + working in-browser demo | Open `index.html` (no build step) |
| `research` branch | Research paper, survey screenshot, transcribed survey (`survey_transcribed.csv`), `SURVEY_FINDINGS.md` | — |
| `DESIGN_SYSTEM.md` | Colors, type, sizes, components and behavior rules shared by all three platforms | — |
| `AI_DISCLOSURE.md` | What AI was used for, and who wrote each part | — |

## What changed in 3.0
- **Same app everywhere.** iOS, Android and the web demo share one design system, the same four tabs (Today, Plan, Focus, Review) and the same engine: ranking, quick-add parsing, spaced repetition and kind streaks.
- **Survey-driven features:**
  - **"Just 5 min" starter.** 39% of students said they delay or distract themselves when stressed.
  - **60-second breathing reset** on breaks. About 1 in 4 asked for something calming.
  - Study Mode nudges students back if they leave, and break tips steer away from short videos (45% game in their free time).
- **Research-driven guardrail.** The paper found AI chatbots can miss signs of crisis, so Avenor has no AI counselor. It links to the 988 Suicide & Crisis Lifeline and encourages students to talk to trusted adults.
- **Data carries over.** Android 3.0 imports tasks, classes and events from version 01. iOS 3.0 reads 2.0 data.

## Verification status
- **Website:** tested in Chromium at desktop and phone widths. The full demo flow works with no console errors and no horizontal scroll.
- **Android:** the engine (`Engine.kt`, `Models.kt`) was compiled with Kotlin 2.0 and its unit tests passed. The store and focus engine were type-checked against API stubs. The Compose UI has **not** been compiled yet (no Android SDK was available), so build it once in Android Studio and fix any diagnostics.
- **iOS:** written against iOS 17+ SwiftUI APIs, but **not compiled yet** (no Xcode was available). Build once in Xcode and fix any diagnostics.

## Privacy and cost
No accounts, servers, analytics or ads. Everything is stored and scheduled on the device. The Android app requests no INTERNET permission. The cost per user is $0. The only fixed cost is store developer fees (Apple $99/yr, which can be waived for qualifying schools or nonprofits, and Google Play's one-time registration fee).

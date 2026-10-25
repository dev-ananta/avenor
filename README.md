# Avenor for Android

This is a Kotlin + Jetpack Compose port of Avenor, synchronized with the iOS app as based on the design methodology of Imran Ahmad.

## Build
1. Open this folder in Android Studio (a 2026 release with AGP 9.3 support).
2. Let Gradle sync, then Run on a device or emulator (Android 8.0 / API 26 or newer).
3. `./gradlew test` runs `EngineTest` (quick-add parsing, kind streak, Leitner).

## Structure
```
avenor/android
│── MainActivity.kt → Root navigation (4 tabs + full-screen Study Mode and review sets)
│── /data
│     └──  Models.kt → (records shared with iOS), AvenorStore.kt (on-device JSON storage + v01 migration)
│── /domain
│     └──  Engine.kt → (Planner, QuickAddParser, Leitner, Streaks), FocusEngine.kt (study-round state machine)
│── /notify
│     └──  Reminders.kt → (AlarmManager local notifications + ReminderReceiver)
│── /ui
│     │──  /theme
│     │──    └── Theme.kt → (design tokens: same values as iOS and the web)
│     │──  /components
│     │──    └── Panel, Chip, buttons, ProgressRing, HoldToEndButton, QuickAddBar, TaskRow, editors
│     └──  /screens
│            └── Today, Plan (Tasks/Calendar/Classes), Focus + Study Mode, Review, You, Onboarding
```

## Notes
- The minimum SDK was raised from 24 to 26 so `java.time` works without desugaring.
- The launcher icon is now the Avenor icon (adaptive, plus a themed monochrome version) instead of the Android template icon.
- Version 01 data in the `avenor_data` preferences is imported automatically on first launch.
- There is no tip jar on Android yet. Google Play requires Play Billing for in-app tips, which would add a library and setup.

##### All Android Code was written by Aoun Jawa. Small portion was assisted by Claude Code to ensure that the code independently developed by Anant Parikh for the iOS users aligned. Once the Design Methodology was synchronized as confirmed by Imran Ahmad the entirety of the project returned to the hands of humanity.

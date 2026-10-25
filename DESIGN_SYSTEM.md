# Avenor design system (shared by iOS, Android and the website)

One set of values, three implementations. If you change a value, change it in all three files:

| Platform | File |
|---|---|
| iOS (SwiftUI) | `iOS-version03/Avenor for iOS/Theme.swift` → `AvenorTheme` |
| Android (Compose) | `Android-version03/app/src/main/java/com/example/avenor/ui/theme/Theme.kt` → `Av` |
| Website (CSS) | `Website-version03/tokens.css` → `--av-*` variables |

## Color
| Token | Hex | Used for |
|---|---|---|
| background | `#08090B` | App and page background (dark only) |
| surface | `#111318` | Cards and panels |
| elevated | `#181B21` | Inputs, secondary buttons, ring tracks |
| border | `#292D35` | 1 px outlines and dividers |
| text | `#F5F5F5` | Primary text |
| muted | `#969BA6` | Secondary text and inactive icons |
| accent | `#8B9CFF` | Primary buttons, selection, links |
| accent-deep | `#5C54F0` | Start of the hero gradient |
| green | `#65D6A0` | Done, success, goal reached |
| red | `#FF7373` | Overdue, high priority, destructive actions |
| orange | `#FFB86B` | Streak, medium priority |
| pink / sky / violet / gold | `#FA8CBE` / `#73CDFA` / `#C8A0FF` / `#FFD666` | Class colors, breathing, review, trophy |

**Class palette (in order):** accent, green, orange, pink, sky, violet, gold, red. A class's color index is the same number on every platform.

**Hero gradient (Von Restorff):** `#5C54F0 → #8B9CFF`, top-left to bottom-right. Use it for **one block per screen at most**: Focus Now on Today, or Ready to Review.

## Type
Each platform uses its native UI font (SF Pro / SF Pro Rounded on Apple, Roboto on Android, the system stack on the web). Titles are 30 pt/dp/px bold, section headings 20 bold, body 15–17, and captions 12.

## Shape and size
- Radii: 14 (small), 16 (buttons and tiles), 20 (panels), 24 (hero cards), 28 (flashcards).
- **Fitts's Law:** primary buttons are full width and 56 tall. The minimum tap target is 44 pt on iOS and the web, and 48 dp on Android (each platform's own guideline).
- The main action on a screen sits in the bottom thumb zone (quick-add bar, Start button).

## Shared components (same name on every platform)
Panel · ScreenTitle · SectionHeading · Chip · PrimaryButton · SecondaryButton · ProgressRing · StatTile · EmptyHint · MasteryDots · HoldToEndButton · QuickAddBar · TaskRow · BreathingReset

## Shared behavior
| Behavior | Rule |
|---|---|
| Navigation | 4 tabs: Today, Plan (Tasks / Calendar / Classes), Focus, Review. Profile opens from the avatar on Today. |
| Ranking | urgency × 2 + priority weight × 1.5 (+0.5 for quick wins). Urgency: overdue 6, today 5, tomorrow 4, 2–3 days 3, 4–7 days 2, later 1, none 0.75. |
| Quick add | `!` = high priority, `30m` / `1.5h` = duration, `today` / `tmrw` / weekday / `10/30` = due date, `#bio` = class. Weekday abbreviations count only when typed in lowercase, so "SAT prep" stays a title. |
| Review | Leitner boxes with intervals of 1, 2, 4, 8 and 16 days. A miss returns in 10 minutes and is re-asked in the same set. Two buttons only. |
| Streak | Active = 5+ focused minutes, a finished task, or a reviewed card. One missed day is forgiven per 7 days of streak. |
| Focus | Presets of 15, 25 and 45 minutes plus the student's default (the web demo adds 1 minute). "Just 5 min" starter. Hold 1.5 s to end early. Every 4th round earns a 15-minute break. |
| Reminders | 7 PM the night before a due date, 7 AM the day of, one daily planning nudge, and a "come back" nudge 90 s after leaving a session. |
| Copy | The same words on every platform: "Focus Now", "Just 5 min", "Lock it in", "Not yet / Got it", "Today's wrap". |

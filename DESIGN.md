# Avenor 2.0 — Design rationale

**Goal:** the app a student opens every day without thinking about it, and the one that makes their study time *work*.
Avenor turns the same brain shortcuts that make social apps sticky toward studying instead of scrolling,
and it never uses them against the student (no ads, no data collection, no paywalls, no guilt).

## 1. The eight UX laws, applied

### Jakob's Law — users expect your app to work like the apps they already use
- Standard iOS tab bar, navigation titles, segmented controls, sheets, swipe actions, context menus and SF Symbols.
- Swipe right = done, swipe left = delete/focus (same as Mail and Reminders). Delete shows an **Undo** toast (Gmail pattern) instead of a scary "permanently delete?" alert.
- Avatar in the top-right opens profile/settings, as in most apps students use.

### Fitts's Law — big, close targets are faster to hit
- Every primary action ("Start focus", "Start review", "Got it") is a full-width 56 pt button.
- The most frequent actions sit in the bottom thumb zone: quick-add bar on Today and Plan, Start button pinned to the bottom of Focus.
- Every tappable element is at least 44 × 44 pt (checkboxes, chevrons, color swatches).
- **Used in reverse:** "End session early" is a 1.5-second *hold* — deliberately harder than continuing. That small friction stops impulsive quitting (VoiceOver users get a normal action).

### Hick's Law — more choices = slower decisions
- 5 tabs → 4 (Tasks, Calendar and Classes now share **Plan**).
- Today shows **one** recommended task and **one** Start button, so a student never stares at a list deciding where to begin.
- Session length: 3 presets, not a number picker. Review grading: 2 buttons ("Not yet" / "Got it"), not a 4-level difficulty scale.

### Miller's Law — people hold about 7 ± 2 items in working memory
- Today: max 3 "up next" tasks and 3 events. Focus: max 5 candidates. Review: sets of 20 cards.
- Task lists are chunked into Overdue / Today / Tomorrow / This week / Later.
- Big tasks are shown as "3 sessions" instead of "75 minutes" — chunks feel doable.
- Card editor nudges "one idea per card".

### Tesler's Law — complexity can't be removed, only moved; move it into the app
- **Natural-language quick add:** `Bio essay fri 45m !` → title, due Friday, 45 min, high priority, Biology class auto-tagged.
- **Auto-prioritizing:** an urgency × importance score picks "Focus Now" and orders every list, with a one-line reason ("Due tomorrow · High priority · 2 sessions").
- Reminders use sensible defaults (7 PM the night before, 7 AM the day of) with no setup screens.
- Class colors, daily goals and session length are pre-chosen from the student's grade.

### Aesthetic-Usability Effect — attractive design is perceived as easier to use
- One calm, dark, low-glare theme; rounded type; consistent radii; per-class colors carried across tasks, events and cards.
- Spring animations, a numeric-transition countdown, symbol bounces and success haptics make finishing things *feel* good.

### Peak-End Rule — people judge an experience by its peak and its end
- Every focus session ends on a celebration screen with stats, streak and daily-goal progress — never just a timer hitting zero.
- Ending early is framed positively ("12 focused minutes still counts"), so the memory of studying isn't a failure.
- Reviews end on "You remembered 14 of 18", with misses framed as how memory gets stronger.
- After 7 PM, Today shows **Today's wrap**: what went right, plus tomorrow's first task (closing the day on a win and pre-deciding the next start).

### Von Restorff Effect — the item that stands out is the one remembered
- The gradient **Focus Now** card (Today) and **Ready to review** card (Review) are the only bright, colored blocks on their screens; everything else is muted.
- Overdue groups are red; high priority gets a single red marker — color is reserved for things that need attention.

## 2. Study science built in ("hacking" the brain in the student's favor)

| Brain tendency | How Avenor uses it |
|---|---|
| Starting is the hardest part (activation energy) | One-tap Start from Today; the task is already chosen. |
| Unfinished tasks nag at us (Zeigarnik effect) | Tasks show "2 sessions left"; the focus ring visibly fills; "Tomorrow starts with…" leaves a deliberate open loop. |
| Effort grows as the goal gets closer (goal-gradient) | Daily ring, countdown copy that intensifies ("Final stretch!"), mastery dots on cards. |
| Testing yourself beats rereading (retrieval practice) | After each session: "Write one question you can now answer" becomes a card. |
| Spaced reviews beat cramming (spacing effect) | Leitner boxes bring cards back after 1, 2, 4, 8, 16 days. |
| Making something yourself makes it memorable (generation effect) | Students write their own questions instead of reading pre-made decks. |
| Pre-committing beats willpower (commitment devices) | Hold-to-quit, "come back" nudge if you leave mid-session, phone-face-down prompt. |
| Daily cues build habits | A daily planning nudge and a **kind streak**: one missed day per week of streak is forgiven, so a sick day doesn't erase 40 days of progress. |

### Ethical guardrails
No infinite feeds, no random rewards or loot boxes, no social comparison, no notifications beyond due dates, session ends and one daily nudge, and nothing is ever locked behind a tip. Avenor collects no data at all, which suits users as young as 13.

## 3. Why it beats the alternatives

| | Avenor 2.0 | Typical planner (e.g. MyStudyLife, myHomework) | Focus timer (e.g. Forest) | Flashcards (e.g. Quizlet, Anki) |
|---|---|---|---|---|
| Plans tasks & classes | ✅ | ✅ | ❌ | ❌ |
| Picks what to do next | ✅ | ❌ | ❌ | ❌ |
| Focus timer tied to a task | ✅ | ❌ / limited | ✅ | ❌ |
| Turns study time into memory cards | ✅ | ❌ | ❌ | Manual |
| Spaced repetition | ✅ | ❌ | ❌ | ✅ |
| Ads / paywall / account | None | Varies | Paid or IAP | Freemium / paid |
| Data collected | None | Account data | Varies | Account data |

## 4. Cheap next steps (all still $0 to run)
1. **Home-screen widget + Live Activity** for the running timer (needs a widget extension target).
2. **iCloud sync** with CloudKit's private database (storage counts against the student's own iCloud, not ours).
3. **Screen Time app-blocking during sessions** with Apple's FamilyControls framework (requires requesting Apple's Family Controls entitlement).
4. Import school calendars from `.ics` links.

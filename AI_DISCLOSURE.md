# AI use disclosure (Congressional App Challenge)

The Congressional App Challenge allows AI tools **only if all AI usage is fully disclosed** in the submission. AI "must not constitute the entirety of the technical development," and students must show significant individual contributions and understanding of their app. *(Quoted from the official CAC rules. Check the 2026 rulebook from your district for the final wording.)*

## Team
| Member | Contribution |
|---|---|
| Anant Parikh (Sophomore, Barrington High School) | iOS app |
| Aoun Jawa (Junior, Barrington High School) | Android app and website |
| Imran Ahmad (Sophomore, Barrington High School) | Design methodology and Business Model Canvas |

## How AI was used
Tool: Claude (Anthropic), used in October 2026.

AI was used **only to sync the Website, Android and iOS versions** so they share the same features, behavior and design. It did not rewrite the apps. Each version was written by the team member listed above.

| Part of the project | Written by | AI-assisted |
|---|---|---|
| iOS app | Anant Parikh | Only for syncing with the Android and Website versions |
| Android app | Aoun Jawa | Only for syncing with the iOS and Website versions |
| Website and in-browser demo | Aoun Jawa | Only for syncing with the iOS and Android versions |
| Design methodology | Imran Ahmad | No |
| Business Model Canvas | Imran Ahmad | No |
| Original idea, research paper, survey design and data collection | Team | No |

## Repository history
This is a new repository that merges the team's previous repositories. The git history of the earlier remote repositories and of the local repositories was not carried over, so commit history here does not show the original development. The repository is organized into these branches:

| Branch | Contents |
|---|---|
| `ios` | iOS app |
| `android` | Android app |
| `site` | Website |
| `research` | Research and survey materials |
| `main` | AI Disclosure, Design System, LICENSE and README.md |

## What the team should be able to explain
- How "Focus Now" picks a task: `Planner.score` in `Engine.swift` / `Engine.kt` / `script.js`
- How the quick-add parser turns "Bio essay fri 45m !" into a task
- How Leitner spaced repetition schedules cards (1, 2, 4, 8, 16 days)
- How the timer stays correct with the screen off (timestamps, not a ticking counter)
- Why there is no AI chatbot in the app (the research finding), and why the app shows 988
- How each of the 8 UX laws shows up in the app (`DESIGN.md` on the `ios` branch)

## Suggested wording for the submission
> "We designed Avenor, wrote the research paper and ran the survey ourselves. Anant Parikh wrote the iOS app, Aoun Jawa wrote the Android app and website, and Imran Ahmad wrote the design methodology and Business Model Canvas. We used Claude, an AI assistant, only to keep the iOS, Android and web versions in sync with each other. It did not rewrite our code. This repository merges our earlier repositories, so their past git history is not included."

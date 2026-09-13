# Avenor Android

A cleaned and modular Android Studio project reconstructed from the supplied single-document source.

## What was fixed

- Removed Markdown formatting accidentally embedded in Kotlin/XML source.
- Split the monolithic source into models, storage, application shell, screens, dialogs, reusable components, and theme files.
- Fixed the Home screen's previously no-op task completion callback.
- Converted mutable model properties to immutable `val` properties and use `copy()` for updates.
- Added safe JSON loading so malformed stored data does not crash the app.
- Added input validation for required task/event/class names.
- Added numeric keyboard and minimum validation for estimated task duration.
- Made long dialog contents scrollable.
- Added a real Compose Material 3 theme rather than relying on conflicting generated theme code.
- Removed unused generated colors and imports.
- Kept the original local SharedPreferences + JSON persistence approach.

## Project structure

```text
Avenor-Android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── androidTest/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/avenor/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/AvenorStorage.kt
│       │   │   ├── model/AvenorModels.kt
│       │   │   └── ui/
│       │   │       ├── AvenorApp.kt
│       │   │       ├── components/
│       │   │       ├── screens/
│       │   │       └── theme/
│       │   └── res/
│       │       └── values/
│       └── test/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

This app uses Jetpack Compose, so screen layouts are Kotlin composables rather than separate XML layout files.

## Open in Android Studio

1. Extract the ZIP.
2. Open the extracted `Avenor-Android` folder in Android Studio.
3. Let Gradle sync.
4. Use an Android emulator or connected device.
5. Run the `app` configuration.

If Android Studio asks to install a missing Android SDK platform/build tool, accept the recommended compatible version.

## Important build note

The source has been reconstructed as a complete, build-ready Android Studio project. The execution environment used to prepare this archive does not contain an Android SDK/Gradle installation, so an actual Android APK compilation could not be performed here. Android Studio should perform the final Gradle sync/build using the versions declared in `app/build.gradle.kts`.

No XML screen layouts are necessary because the original application was written with Jetpack Compose.

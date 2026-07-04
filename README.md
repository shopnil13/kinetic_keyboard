# Kinetic Keyboard

An Android keyboard (IME) for **Bangla (UniJoy fixed), Banglish (phonetic), and English**, replicating the classic Ridmik/Avro "ইউনিজয়" layout with a modern Kotlin + Jetpack Compose implementation. Goal: match or beat Gboard for Bengali users.

- **Full spec & live build tracker:** [SPEC.md](SPEC.md) — §10 is the task-by-task blueprint.
- **Authoritative layouts** (extracted from the original app): [reference/EXTRACTED_LAYOUTS.md](reference/EXTRACTED_LAYOUTS.md)
- **Recovered phonetic engine** to port: [reference/RidmikParser.java](reference/RidmikParser.java), [reference/BanglaUnicode.java](reference/BanglaUnicode.java)

## Tech
Kotlin · Jetpack Compose · `InputMethodService` · minSdk 28 (Android 9) · compile/target SDK 35 · JDK 17 · AGP 8.7 / Gradle 8.9.

## Build & run (Phase 0)
1. Open this folder in **Android Studio** (Ladybug or newer). Let it sync; it will download Gradle 8.9 and the SDK components, and create `local.properties`.
   - If `./gradlew` reports a missing wrapper jar, run **`gradle wrapper`** once (or use Android Studio's bundled Gradle).
2. Run the **app** configuration on an emulator or device (installs the setup screen).
3. On the device: **Enable in Settings** → turn on Kinetic Keyboard → **Choose Keyboard** → select it.
4. Tap the test field and type — the placeholder keys should insert text.

## Status
Phase 0 (scaffold + Compose-in-IME) — code in place; build/verify on a machine with the Android SDK. See [SPEC.md](SPEC.md) §10.1 dashboard.

## Project layout
```
app/src/main/
├── AndroidManifest.xml
├── java/com/kinetic/keyboard/
│   ├── MainActivity.kt              # setup / test screen
│   ├── service/KeyboardImeService.kt
│   ├── service/ImeLifecycleOwner.kt # Compose-in-IME plumbing (SPEC §4.1)
│   └── ui/MinimalKeyboard.kt        # Phase-0 placeholder UI
└── res/xml/method.xml               # IME subtypes (bn unijoy / bn phonetic / en)
```

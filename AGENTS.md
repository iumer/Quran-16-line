# Quran 16-Line (Android)

Native Android reader (Kotlin + Jetpack Compose) for the Taj Company 16-line mushaf PDF.
Single Gradle module `:app` (`com.quran16line.app`), `minSdk 26`, `targetSdk 34`, `compileSdk 34`.

## Cursor Cloud specific instructions

- This is a GUI **Android** app. The cloud VM has **no `/dev/kvm`**, so a hardware-accelerated
  Android emulator cannot run here. Do not rely on booting an emulator to verify changes;
  a software (TCG/SwiftShader) emulator is impractical on this VM. Verify work by building the
  APK and running the JVM unit tests (`app/src/test/...`), which exercise the app's real core
  logic (surah/ayah/page search navigation, reading-resume, and line-highlight tap mapping).
- The Android SDK is installed at `$HOME/android-sdk` and is referenced by `local.properties`
  (`sdk.dir=...`). `local.properties` is git-ignored; the startup update script recreates it,
  so you normally don't need to touch it. `ANDROID_HOME`/`PATH` are also exported in `~/.bashrc`
  for interactive shells.
- JDK 21 is the system default and builds the project fine even though the module targets Java 17
  bytecode. No separate JDK 17 install is required.
- Standard commands (run from repo root):
  - Build debug APK: `./gradlew assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`).
  - Unit tests: `./gradlew test` (or `./gradlew testDebugUnitTest`). HTML report:
    `app/build/reports/tests/testDebugUnitTest/index.html`.
  - Lint: `./gradlew lintDebug` (report: `app/build/reports/lint-results-debug.html`).
  - Release APK (per README): `./gradlew assembleRelease` (unsigned unless a keystore is provided).
- First Gradle invocation downloads the Gradle 8.7 distribution and dependencies, so it is slow;
  subsequent runs are cached. Adding `--no-daemon` avoids a lingering daemon in short-lived VMs.

# 🎸 Fretboard Trainer - Android Version

**Language:** English • [Español (Spanish)](es/ANDROID.md)

This directory (`android/`) contains the native Android application for **Fretboard Trainer**, engineered with **Jetpack Compose** and **Material 3**. It directly reuses the musical model, adaptive learning training engine, and real-time DSP pitch detection algorithm (YIN autocorrelation) from the root Java codebase.

---

## 📥 Download Precompiled APK

You do not need to build from source to try the app on your Android device:

* **Direct Download**: Download the latest APK from [GitHub Releases](https://github.com/Gaboxondo/Fretboard--rainer/releases/latest) by grabbing **`FretboardTrainer-Android.apk`**.
* **Installation**: Open the APK file on your Android device. If prompted by Android, enable "Install unknown apps" for your web browser or file manager.

---

## 🏗️ Code Reuse & Technical Architecture

The Android application leverages Gradle `sourceSets` to directly compile shared Java classes from `../../src/main/java`, guaranteeing parity with the desktop application and preventing code duplication:

```
ThreadBoardlearn/
├── src/main/java/com/threadboardlearn/
│   ├── model/         <-- 100% Shared (Note, GuitarString, Tuning, TrainingConfig)
│   ├── engine/        <-- 100% Shared (TrainingSession, AdaptiveLearningEngine, Stats)
│   ├── dsp/           <-- 100% Shared (YinPitchDetector, PitchDetectionResult)
│   ├── i18n/          <-- Shared (MessageBundle, language resources)
│   ├── audio/         <-- Abstract interfaces (AudioPlayerService, MicrophoneCaptureService)
│   └── ui/            <-- Desktop-only (Swing / FlatLaf)
│
└── android/
    ├── app/src/main/java/com/threadboardlearn/android/
    │   ├── audio/
    │   │   ├── AndroidAudioRecordCapture.kt  (Implements MicrophoneCaptureService with AudioRecord)
    │   │   └── AndroidAudioPlayer.kt         (Implements AudioPlayerService with AudioTrack)
    │   ├── ui/
    │   │   └── FretboardComposable.kt        (Tactile interactive fretboard in Jetpack Compose)
    │   └── MainActivity.kt                   (Main screen with Jetpack Compose & Material 3)
```

---

## 📱 Features of the Android Version

1. **Interactive Touch Fretboard:**
   - Vector rendering of all 6 strings with proportional gauges and 12 frets with standard position markers (inlays on frets 3, 5, 7, 9, and double dots on fret 12).
   - Tap any string and fret directly on screen to answer challenge notes.
   - Instant visual and haptic feedback (green for correct, red for misses).

2. **Real-Time Microphone Pitch Detection:**
   - Linear PCM audio capture using `android.media.AudioRecord` at 44.1 kHz.
   - Audio buffers are fed directly into the shared `YinPitchDetector` without modifications.
   - Practice with acoustic, classical, or electric guitar (via USB audio interface or built-in microphone).
   - Live VU audio level meter and real-time detected note badge.

3. **Smart Adaptive Learning Engine & Progressive String Selection:**
   - Tracks misses, correct answers, and response times to dynamically weight and prioritize notes you find most challenging.
   - Allows selecting any custom combination of strings (e.g., 6th and 5th strings together), a single string, or all 6 strings for progressive fretboard mastery.

4. **Acoustic Synthesizer & Audio Feedback:**
   - Acoustic harmonic synthesis with realistic string decay and success/failure audio cues via `AudioTrack`.
   - Automatic microphone suppression while reference notes are playing to avoid false pitch triggers.

5. **Multi-Language Support (i18n):**
   - Automatically detects system locale (English or Spanish) and provides an in-app language switch selector in the top bar.

---

## 🚀 Opening and Building in Android Studio

1. Open **Android Studio** (Iguana, Jellyfish, or newer).
2. Select **File > Open** and choose the `android/` directory inside this repository.
3. Allow Android Studio to automatically sync the Gradle project.
4. Connect a physical Android device with USB debugging enabled (or launch an Android emulator with API 26+).
5. Click **Run 'app'** (`Shift + F10`).

---

## 🛠️ Command-Line Build

From the `android/` directory:

```bash
# On Windows (PowerShell / CMD):
.\gradlew.bat assembleDebug

# On Linux / macOS:
./gradlew assembleDebug
```

The resulting debug APK is located at:
`android/app/build/outputs/apk/debug/app-debug.apk`

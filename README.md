# Guitar Fretboard Trainer (ThreadBoardLearn) 🎸

Desktop application built with **Java 21** and **Maven** for interactive training and memorization of notes on the electric and acoustic guitar fretboard. Inspired by modern fretboard learning methods like [FaChords Guitar Fretboard Notes](https://www.fachords.com/guitar-fretboard-notes/).

---

## 📋 Project Overview

The goal is to allow guitarists of any skill level to master fretboard notes in an agile, gamified way:
1. The application presents a random challenge: a **target note** (e.g. `C`, `F#`, etc.) and a **target string** (or the fixed string chosen in settings).
2. The reference note tone is played acoustically.
3. The user has a configurable **X seconds** countdown timer to play the correct note on their real guitar.
4. The application listens through the **computer microphone** using real-time digital signal processing (DSP) and pitch detection. When the correct frequency is detected, it validates the hit and advances to the next challenge.
5. It also allows responding interactively by clicking directly on the visual fretboard on screen.

---

## 🎵 Musical Foundations and System Rules

### 1. Note System (Standard Western / Anglo-Saxon Notation)
- **Natural notes**: `A`, `B`, `C`, `D`, `E`, `F`, `G`
- **Sharps (#)**: `A#`, `C#`, `D#`, `F#`, `G#`
- **Natural semitone rule**: Between `B` and `C`, and between `E` and `F`, there is a half step (natural diatonic semitone), so **there are no `B#` or `E#`** in the standard working scale.
- **Total chromatic semitones (12)**:
  $$\text{C} \rightarrow \text{C}\# \rightarrow \text{D} \rightarrow \text{D}\# \rightarrow \text{E} \rightarrow \text{F} \rightarrow \text{F}\# \rightarrow \text{G} \rightarrow \text{G}\# \rightarrow \text{A} \rightarrow \text{A}\# \rightarrow \text{B}$$

### 2. Standard Guitar Tuning (6 Strings)
From lowest pitch (thickest) to highest pitch (thinnest):
| String | Open Note | Fundamental Frequency ($f_0$) | Scientific Pitch Notation |
| :---: | :---: | :---: | :---: |
| **6th** | **E** (Low E) | ~82.41 Hz | E2 |
| **5th** | **A** | ~110.00 Hz | A2 |
| **4th** | **D** | ~146.83 Hz | D3 |
| **3rd** | **G** | ~196.00 Hz | G3 |
| **2nd** | **B** | ~246.94 Hz | B3 |
| **1st** | **E** (High E) | ~329.63 Hz | E4 |

Each fret increases the frequency by one semitone:
$$f = f_{\text{string}} \times 2^{\frac{\text{fret}}{12}}$$

---

## ⚙️ Configurable Options and Levels

1. **String Selection**:
   - Single fixed string: Practice exclusively on the 6th, 5th, 4th, 3rd, 2nd, or 1st string.
   - "All strings" mode: In each round, a string is chosen at random along with the note.
2. **Include Accidentals (Sharps #)**:
   - *Disabled*: Natural notes only (`A, B, C, D, E, F, G`).
   - *Enabled*: Sharps are also included (`A#, C#, D#, F#, G#`).
3. **Response Time (Timer)**:
   - Configurable from 2 to 30 seconds (or untimed / relaxed mode).
   - Visual countdown progress bar.
4. **Training Fret Range**:
   - Frets 0 to 12 (standard range with double fret markers at the 12th fret).
   - Additional filters: Open frets (0–5), middle (5–9), high (9–12), or full neck up to 15/24.
5. **User Input**:
   - **Microphone (Pitch Detection)**: Listens to the real instrument via default or selected audio input device.
   - **Interactive Fretboard**: Allows clicking with the mouse on the corresponding fret and string as an alternative or complement.
6. **Reference Audio**:
   - Enable or disable acoustic reference tone playback at the start of each round.

---

## 🏗️ Technical Architecture

The application is structured into decoupled layers leveraging modern **Java 21** features:

```
com.threadboardlearn
├── App.java                      # Swing entry point with FlatLaf Dark Theme
├── model/
│   ├── Note.java                 # Note enum / record (name, semitone, accidentals)
│   ├── GuitarString.java         # Guitar string definition (index 1-6, base tuning)
│   ├── FretboardPosition.java    # Pair (string, fret, resulting note, Hz frequency)
│   └── TrainingSessionConfig.java# Settings (fixed string or all, with sharps, timer, etc.)
├── audio/
│   ├── AudioPlayer.java          # Tone synthesis / Acoustic synthesized reference audio
│   └── MicrophoneCapture.java    # Continuous PCM capture using javax.sound.sampled (TargetDataLine)
├── dsp/
│   ├── PitchDetector.java        # Pitch detection interface
│   └── YinPitchDetector.java     # YIN algorithm / Autocorrelation implementation for guitar
├── engine/
│   ├── GameSession.java          # Game session logic, random generator, timers, and events
│   └── GameEventListener.java    # UI notifications (new note, timer tick, hit, miss)
└── ui/
    ├── MainFrame.java            # Main application window
    ├── ConfigPanel.java          # Configuration panel for levels, strings, timer, microphone
    ├── ChallengeDisplayPanel.java# Prominent note letter, target string, and countdown timer
    └── FretboardView.java        # Custom GUI component rendering the guitar fretboard
```

### Key Technologies:
- **Java 21 LTS**: Records, exhaustive pattern matching/switch expressions, Virtual Threads for continuous audio processing without blocking Swing's Event Dispatch Thread (EDT).
- **Project Lombok**: Boilerplate reduction using `@Getter` and `@Setter` on models and engines.
- **FlatLaf (Dark / Modern UI)**: Professional visual theme for modern desktop applications.
- **Java Sound API (`javax.sound.sampled`)**:
  - `TargetDataLine` for PCM audio capture (44.1 kHz, 16-bit mono).
  - `SourceDataLine` for synthesizing sinusoidal waves with acoustic guitar-like harmonics and decay envelopes.
- **YIN Algorithm / Autocorrelation**:
  - Tuned specifically for guitar pitch range (~70 Hz to ~1000 Hz).
  - Clarity and energy thresholds to discard ambient background noise.

---

## 🚀 Requirements and Getting Started

### Requirements:
- **Windows 10 / 11** (or macOS / Linux)
- **JDK 21** (e.g. Amazon Corretto 21, Eclipse Temurin 21)
- **Apache Maven 3.8+**
- Microphone or audio interface (optional, but recommended for listening mode).

### Building and Running:

1. **Verify or configure Java 21 in your terminal**:
   ```powershell
   $env:JAVA_HOME = "C:\Devtools\jdks\Corretto21"
   $env:Path = "C:\Devtools\jdks\Corretto21\bin;$env:Path"
   java -version
   mvn -v
   ```

2. **Build the project**:
   ```powershell
   mvn clean package
   ```

3. **Run the application**:
   ```powershell
   mvn exec:java -Dexec.mainClass="com.threadboardlearn.App"
   # Or run the generated Fat JAR:
   java -jar target/fretboard-trainer-1.0.0.jar
   ```

---

## 📌 Project Status and Roadmap
- [x] Base setup in `pom.xml` with Java 21, FlatLaf, and Maven plugins.
- [x] Master specification and architecture documentation (`README.md`).
- [x] Music domain model implementation ([Note.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/model/Note.java), [GuitarString.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/model/GuitarString.java), [FretboardPosition.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/model/FretboardPosition.java)).
- [x] Audio synthesis implementation for acoustic reference tone and sound feedback ([AudioPlayer.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/audio/AudioPlayer.java)).
- [x] Microphone capture ([MicrophoneCapture.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/audio/MicrophoneCapture.java)) and YIN pitch detection algorithm ([YinPitchDetector.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/dsp/YinPitchDetector.java)).
- [x] Training session and randomized challenge engine ([TrainingSession.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/engine/TrainingSession.java), [Challenge.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/engine/Challenge.java), [TrainingStats.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/engine/TrainingStats.java)).
- [x] Graphical fretboard component ([FretboardView.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/FretboardView.java)) featuring fret inlays, proportional string gauges, and interactive mouse-click detection.
- [x] Complete graphical user interface ([MainFrame.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/MainFrame.java), [ChallengeDisplayPanel.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/ChallengeDisplayPanel.java), [ControlPanel.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/ControlPanel.java), [StatusBar.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/StatusBar.java)).
- [x] Automated unit test suite for notes, string tuning, and YIN pitch detection (11 tests passing).
- [x] Executable Fat JAR packaging and one-click launch scripts (`run.bat`, `run.ps1`).

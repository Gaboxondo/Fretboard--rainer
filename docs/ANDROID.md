# Fretboard Trainer - Versión Android

Esta carpeta (`android/`) contiene la aplicación nativa para Android de **Fretboard Trainer**, desarrollada con **Jetpack Compose** y reutilizando la lógica musical, el motor de entrenamiento y el algoritmo de detección de tono por micrófono (YIN DSP) del repositorio principal.

---

## 🏗️ Arquitectura de Reutilización

El proyecto Android aprovecha la configuración `sourceSets` de Gradle para compilar directamente los paquetes Java compartidos desde `../../src/main/java`, evitando duplicidad de código:

```
ThreadBoardlearn/
├── src/main/java/com/threadboardlearn/
│   ├── model/         <-- Compartido al 100% (Note, GuitarString, Tuning, TrainingConfig)
│   ├── engine/        <-- Compartido al 100% (TrainingSession, AdaptiveLearningEngine, Stats)
│   ├── dsp/           <-- Compartido al 100% (YinPitchDetector, PitchDetectionResult)
│   ├── i18n/          <-- Compartido (MessageBundle, recursos)
│   ├── audio/         <-- Interfaces abstractas (AudioPlayerService, MicrophoneCaptureService)
│   └── ui/            <-- Exclusivo de Desktop (Swing / FlatLaf)
│
└── android/
    ├── app/src/main/java/com/threadboardlearn/android/
    │   ├── audio/
    │   │   ├── AndroidAudioRecordCapture.kt  (Implementa MicrophoneCaptureService con AudioRecord)
    │   │   └── AndroidAudioPlayer.kt         (Implementa AudioPlayerService con AudioTrack)
    │   ├── ui/
    │   │   └── FretboardComposable.kt        (Diapasón interactivo táctil en Jetpack Compose)
    │   └── MainActivity.kt                   (Pantalla principal con Jetpack Compose y Material 3)
```

---

## 📱 Características de la Versión Android

1. **Diapasón Táctil Interactivo:**
   - Dibuja las 6 cuerdas y los 12 trastes con marcadores tradicionales (puntos en trastes 3, 5, 7, 9 y doble punto en 12).
   - Toca cualquier cuerda y traste en la pantalla para responder a los retos.
   - Feedback táctil y visual inmediato (verde para aciertos, rojo para fallos).

2. **Detección de Tono por Micrófono en Tiempo Real:**
   - Utiliza `android.media.AudioRecord` a 44.1 kHz.
   - Pasa los buffers de audio directamente al detector `YinPitchDetector` original sin cambios.
   - Permite practicar con una guitarra acústica o eléctrica real conectada o al aire.
   - Incluye medidor VU de señal y afinador en pantalla.

3. **Motor de Aprendizaje Adaptativo:**
   - Registra fallos, aciertos y tiempos de respuesta para ponderar las notas con las que más dificultad tiene el usuario.

4. **Feedback de Audio:**
   - Síntesis de armónicos con decaimiento natural de cuerda de guitarra y arpegios de acierto/fallo mediante `AudioTrack`.

---

## 🚀 Cómo abrir y compilar en Android Studio

1. Abre **Android Studio**.
2. Selecciona **File > Open** y elige la carpeta `c:\Proyectos\ThreadBoardlearn\android`.
3. Android Studio sincronizará el proyecto Gradle automáticamente.
4. Conecta un dispositivo Android (o inicia un emulador) y pulsa **Run 'app'** (`Shift + F10`).

---

## 🛠️ Compilación por Línea de Comandos

Desde la carpeta `android/`:

```bash
# En Windows:
gradlew.bat assembleDebug

# En Linux / macOS:
./gradlew assembleDebug
```

El APK generado se encontrará en:
`android/app/build/outputs/apk/debug/app-debug.apk`

# 🎸 Fretboard Trainer - Versión Android

**Idioma:** [English (Inglés)](../ANDROID.md) • **Español**

Esta carpeta (`android/`) contiene la aplicación nativa para Android de **Fretboard Trainer**, desarrollada con **Jetpack Compose** y **Material 3**. Reutiliza directamente la lógica musical, el motor de entrenamiento adaptativo y el algoritmo de procesamiento digital de señales (DSP YIN) para detección de tono por micrófono desde el repositorio principal en Java.

---

## 📥 Descarga del APK Precompilado

No es necesario compilar el proyecto para probar la aplicación en tu móvil o tablet Android:

* **Descarga directa**: Puedes descargar el archivo APK más reciente desde [GitHub Releases](https://github.com/Gaboxondo/Fretboard--rainer/releases/latest) buscando **`FretboardTrainer-Android.apk`**.
* **Instalación**: Abre el archivo APK en tu dispositivo Android. Si el sistema te lo solicita, autoriza "Instalar aplicaciones desconocidas" en tu navegador o explorador de archivos.

---

## 🏗️ Arquitectura de Reutilización de Código

El proyecto Android aprovecha la configuración `sourceSets` de Gradle para compilar directamente los paquetes Java compartidos desde `../../src/main/java`, garantizando consistencia absoluta con la versión de escritorio y eliminando duplicación de código:

```
ThreadBoardlearn/
├── src/main/java/com/threadboardlearn/
│   ├── model/         <-- Compartido al 100% (Note, GuitarString, Tuning, TrainingConfig)
│   ├── engine/        <-- Compartido al 100% (TrainingSession, AdaptiveLearningEngine, Stats)
│   ├── dsp/           <-- Compartido al 100% (YinPitchDetector, PitchDetectionResult)
│   ├── i18n/          <-- Compartido (MessageBundle, recursos de idioma)
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
   - Renderiza las 6 cuerdas con calibres proporcionales y 12 trastes con marcadores tradicionales (inlays en trastes 3, 5, 7, 9 y doble punto en 12).
   - Permite tocar cualquier traste y cuerda en la pantalla táctil para responder a los retos interactivos.
   - Retroalimentación táctil y visual instantánea (verde para notas acertadas, rojo para fallos).

2. **Detección de Tono por Micrófono en Tiempo Real:**
   - Captura de audio PCM lineal mediante `android.media.AudioRecord` a 44.1 kHz.
   - Envía los búferes de audio directamente al detector `YinPitchDetector` original sin alteraciones.
   - Permite entrenar con guitarra acústica, clásica o eléctrica (mediante interfaz de audio USB o micrófono del dispositivo).
   - Incluye medidor visual VU de nivel de entrada e indicación de nota detectada en vivo.

3. **Motor de Aprendizaje Adaptativo y Selección Progresiva de Cuerdas:**
   - Registra fallos, aciertos y tiempos de reacción individuales para priorizar automáticamente aquellas notas donde el usuario muestra mayor dificultad o vacilación.
   - Permite seleccionar cualquier combinación personalizada de cuerdas (por ejemplo, practicar solo la 6ª y 5ª cuerda), una cuerda individual o las 6 cuerdas para un aprendizaje progresivo.

4. **Sintetizador y Efectos de Audio:**
   - Generación de armónicos acústicos con decaimiento natural y sonidos de refuerzo mediante `AudioTrack`.
   - Silenciamiento automático del micrófono durante la reproducción de la nota de referencia para prevenir falsos positivos.

5. **Soporte Multiidioma (i18n):**
   - Detección automática del idioma del dispositivo (Español o Inglés) y selector directo en la barra superior.

---

## 🚀 Cómo abrir y compilar en Android Studio

1. Abre **Android Studio** (versión Iguana, Jellyfish o superior).
2. Selecciona **File > Open** y navega a la subcarpeta `android/` del repositorio.
3. Espera a que Android Studio sincronice el proyecto Gradle automáticamente.
4. Conecta un dispositivo físico Android con depuración USB habilitada (o inicia un emulador Android con API 26+).
5. Pulsa el botón **Run 'app'** (`Shift + F10`).

---

## 🛠️ Compilación por Línea de Comandos

Desde la carpeta `android/`:

```bash
# En Windows (PowerShell / CMD):
.\gradlew.bat assembleDebug

# En Linux / macOS:
./gradlew assembleDebug
```

El archivo APK generado se ubicará en:
`android/app/build/outputs/apk/debug/app-debug.apk`


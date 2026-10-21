# Guitar Fretboard Trainer (ThreadBoardLearn) 🎸

Aplicación de escritorio en **Java 21** y **Maven** para el entrenamiento y memorización interactiva de las notas en el diapasón de la guitarra eléctrica/acústica. Inspirada en métodos de aprendizaje modernos como [FaChords Guitar Fretboard Notes](https://www.fachords.com/guitar-fretboard-notes/).

---

## 📋 Resumen del Proyecto

El objetivo es permitir a guitarristas de cualquier nivel dominar las notas en el diapasón de forma ágil y gamificada:
1. La aplicación presenta un reto aleatorio: una **nota objetivo** (ej. `C`, `F#`, etc.) y una **cuerda específica** (o la cuerda fijada en la configuración).
2. Se reproduce acústicamente el sonido de la nota de referencia.
3. El usuario dispone de **X segundos** configurables para tocar la nota correcta en su guitarra real.
4. La aplicación escucha a través del **micrófono del ordenador** mediante procesamiento de señal y detección de tono en tiempo real (*pitch detection*). Si detecta la frecuencia correcta, valida el acierto y avanza a la siguiente nota aleatoria.
5. También permite responder de forma interactiva haciendo clic directamente sobre el diapasón visual en la pantalla.

---

## 🎵 Fundamentos Musicales y Reglas del Sistema

### 1. Sistema de Notas (Notación Anglosajona / Cifrado Americano)
- **Notas naturales**: `A`, `B`, `C`, `D`, `E`, `F`, `G`
- **Sostenidos (#)**: `A#`, `C#`, `D#`, `F#`, `G#`
- **Regla de semitonos naturales**: Entre `B` y `C`, y entre `E` y `F` hay medio tono (semitono diatónico natural), por lo que **no existen `B#` ni `E#`** en la escala de trabajo estándar.
- **Total de semitonos cromáticos (12)**:
  $$\text{C} \rightarrow \text{C}\# \rightarrow \text{D} \rightarrow \text{D}\# \rightarrow \text{E} \rightarrow \text{F} \rightarrow \text{F}\# \rightarrow \text{G} \rightarrow \text{G}\# \rightarrow \text{A} \rightarrow \text{A}\# \rightarrow \text{B}$$

### 2. Afinación Estándar de Guitarra (6 Cuerdas)
De más gruesa (grave) a más fina (aguda):
| Cuerda | Nota al aire | Frecuencia fundamental ($f_0$) | Notación científica |
| :---: | :---: | :---: | :---: |
| **6ª** | **E** (Mi grave) | ~82.41 Hz | E2 |
| **5ª** | **A** (La) | ~110.00 Hz | A2 |
| **4ª** | **D** (Re) | ~146.83 Hz | D3 |
| **3ª** | **G** (Sol) | ~196.00 Hz | G3 |
| **2ª** | **B** (Si) | ~246.94 Hz | B3 |
| **1ª** | **E** (Mi agudo) | ~329.63 Hz | E4 |

Cada traste incrementa la frecuencia en un semitono:
$$f = f_{\text{cuerda}} \times 2^{\frac{\text{traste}}{12}}$$

---

## ⚙️ Niveles y Opciones Configurables

1. **Selección de Cuerda**:
   - Cuerda fija individual: Practicar exclusivamente la 6ª, 5ª, 4ª, 3ª, 2ª o 1ª cuerda.
   - Modo "Todas las cuerdas": En cada ronda se escoge una cuerda al azar junto con la nota.
2. **Inclusión de Alteraciones (Sharps #)**:
   - *Desactivado*: Solo notas naturales (`A, B, C, D, E, F, G`).
   - *Activado*: Se incluyen también los sostenidos (`A#, C#, D#, F#, G#`).
3. **Tiempo de Respuesta (Temporizador)**:
   - Configurable de 2 a 30 segundos (o modo sin temporizador / relajado).
   - Barra de progreso visual con cuenta atrás.
4. **Rango de Trastes de Entrenamiento**:
   - Trastes 0 a 12 (rango estándar con puntos dobles en el 12).
   - Filtros adicionales: Trastes abiertos (0–5), medios (5–9), agudos (9–12), o completo hasta el 15/24.
5. **Entrada de Usuario**:
   - **Micrófono (Pitch Detection)**: Escucha el instrumento real vía la tarjeta de sonido por defecto o seleccionada.
   - **Diapasón Interactivo**: Permite hacer clic con el ratón sobre el traste y cuerda correspondiente como alternativa o complemento.
6. **Audio de Referencia**:
   - Activación/desactivación de la reproducción del tono objetivo al inicio de cada ronda.

---

## 🏗️ Arquitectura Técnica de la Aplicación

La aplicación se estructura en capas desacopladas aprovechando las características modernas de **Java 21**:

```
com.threadboardlearn
├── App.java                      # Punto de entrada Swing con FlatLaf Dark Theme
├── model/
│   ├── Note.java                 # Enum o Record de notas (nombre, semitono, alteraciones)
│   ├── GuitarString.java         # Definición de cuerdas (índice 1-6, afinación base)
│   ├── FretboardPosition.java    # Par (cuerda, traste, nota resultante, frecuencia Hz)
│   └── TrainingSessionConfig.java# Ajustes (cuerda fija o all, con sharps, tiempo, etc.)
├── audio/
│   ├── AudioPlayer.java          # Síntesis de tonos / Audio sintetizado de referencia
│   └── MicrophoneCapture.java    # Captura PCM continua mediante javax.sound.sampled (TargetDataLine)
├── dsp/
│   ├── PitchDetector.java        # Interfaz de detección de tono
│   └── YinPitchDetector.java     # Implementación del algoritmo YIN / Autocorrelación para guitarra
├── engine/
│   ├── GameSession.java          # Lógica de juego, generación aleatoria, temporizadores y eventos
│   └── GameEventListener.java    # Notificaciones a la UI (nueva nota, tiempo, acierto, error)
└── ui/
    ├── MainFrame.java            # Ventana principal
    ├── ConfigPanel.java          # Panel de configuración de niveles, cuerdas, tiempo, micrófono
    ├── ChallengeDisplayPanel.java# Letra gigante de la nota, cuerda objetivo y cronómetro
    └── FretboardView.java        # Componente gráfico personalizado con dibujo del diapasón
```

### Tecnologías Clave:
- **Java 21 LTS**: Records, switch expressions exhaustivos, Virtual Threads para el procesamiento de audio continuo sin bloquear el Event Dispatch Thread (EDT) de Swing.
- **Project Lombok**: Reducción de código repetitivo mediante `@Getter` y `@Setter` en modelos y motores.
- **FlatLaf (Dark / Modern UI)**: Aspecto visual profesional para aplicaciones de escritorio.
- **Java Sound API (`javax.sound.sampled`)**:
  - `TargetDataLine` para captura de audio PCM (44.1 kHz, 16-bit mono).
  - `SourceDataLine` para reproducción de ondas sinusoidales con armónicos y envolvente tipo guitarra acústica.
- **Algoritmo YIN / Autocorrelación**:
  - Especialmente optimizado para el rango de guitarra (~70 Hz a ~1000 Hz).
  - Umbral de claridad/energía para descartar ruido ambiental.

---

## 🚀 Requisitos y Ejecución

### Requisitos:
- **Windows 10 / 11**
- **JDK 21** (instalado en el sistema, ej. Amazon Corretto 21)
- **Apache Maven 3.8+**
- Micrófono o interfaz de audio (opcional pero recomendado para el modo escucha).

### Compilación y Ejecución:

1. **Verificar o configurar Java 21 en la terminal**:
   ```powershell
   $env:JAVA_HOME = "C:\Devtools\jdks\Corretto21"
   $env:Path = "C:\Devtools\jdks\Corretto21\bin;$env:Path"
   java -version
   mvn -v
   ```

2. **Compilar el proyecto**:
   ```powershell
   mvn clean package
   ```

3. **Ejecutar la aplicación**:
   ```powershell
   mvn exec:java -Dexec.mainClass="com.threadboardlearn.App"
   # O ejecutar el JAR generado:
   java -jar target/fretboard-trainer-1.0.0.jar
   ```

---

## 📌 Historial de Decisiones y Estado
- [x] Configuración base de `pom.xml` con Java 21, FlatLaf y plugins de Maven.
- [x] Documento maestro de especificación y requerimientos (`README.md`).
- [x] Implementación del modelo de dominio musical ([Note.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/model/Note.java), [GuitarString.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/model/GuitarString.java), [FretboardPosition.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/model/FretboardPosition.java)).
- [x] Implementación de síntesis de audio para reproducir la nota con sonido de guitarra y feedback ([AudioPlayer.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/audio/AudioPlayer.java)).
- [x] Implementación de captura de micrófono ([MicrophoneCapture.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/audio/MicrophoneCapture.java)) y algoritmo YIN ([YinPitchDetector.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/dsp/YinPitchDetector.java)).
- [x] Diseño del motor de juego y retos aleatorios ([TrainingSession.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/engine/TrainingSession.java), [Challenge.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/engine/Challenge.java), [TrainingStats.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/engine/TrainingStats.java)).
- [x] Componente gráfico de diapasón ([FretboardView.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/FretboardView.java)) con marcadores inlays, cuerdas proporcionales y detección interactiva por clic.
- [x] Interfaz gráfica completa ([MainFrame.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/MainFrame.java), [ChallengeDisplayPanel.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/ChallengeDisplayPanel.java), [ControlPanel.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/ControlPanel.java), [StatusBar.java](file:///c:/Proyectos/ThreadBoardlearn/src/main/java/com/threadboardlearn/ui/StatusBar.java)).
- [x] Pruebas unitarias automatizadas para notas, afinación de cuerdas y detección YIN (11 tests pasando).
- [x] Empaquetado Fat JAR ejecutable y scripts de arranque directo (`run.bat`, `run.ps1`).

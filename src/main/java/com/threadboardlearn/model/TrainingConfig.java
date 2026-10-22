package com.threadboardlearn.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Parámetros de configuración de la sesión de entrenamiento.
 */
@Getter
@Setter
public class TrainingConfig {
    private GuitarString selectedString; // null = Todas las cuerdas aleatoriamente
    private boolean includeSharps;       // true = incluye A#, C#, D#, F#, G#
    private int timeLimitSeconds;        // Segundos por reto (ej. 5, 0 = sin límite)
    private int maxFret;                 // Traste máximo (por defecto 12)
    private boolean playReferenceAudio;  // Reproduce la nota al inicio
    private boolean microphoneEnabled;   // Escucha por el micrófono
    private double micSensitivity;       // Umbral de sensibilidad / noise gate para el micro (RMS)
    private boolean matchExactOctave;    // true = exige la frecuencia y octava de la cuerda de guitarra
    private double minConfidence;        // Confianza mínima del detector (0.0 a 1.0)
    private boolean adaptiveLearning;    // true = Selección probabilística ponderada según fallos y tiempos

    public TrainingConfig() {
        this.selectedString = GuitarString.STRING_6; // 6ª Cuerda (grave) por defecto
        this.includeSharps = false; // Solo naturales por defecto
        this.timeLimitSeconds = 3;  // 3 segundos por defecto
        this.maxFret = 12;
        this.playReferenceAudio = true;
        this.microphoneEnabled = true;
        this.micSensitivity = 0.045; // Puerta de ruido para ignorar voces de fondo
        this.matchExactOctave = true; // Exigir octava real de la cuerda
        this.minConfidence = 0.75;
        this.adaptiveLearning = true; // Activo por defecto para optimizar el aprendizaje
    }

    public boolean isAllStrings() {
        return selectedString == null;
    }

    public void setAllStrings(boolean all) {
        if (all) {
            this.selectedString = null;
        } else if (this.selectedString == null) {
            this.selectedString = GuitarString.STRING_6;
        }
    }

    public GuitarString getSelectedString() { return selectedString; }
    public void setSelectedString(GuitarString selectedString) { this.selectedString = selectedString; }

    public boolean isIncludeSharps() { return includeSharps; }
    public void setIncludeSharps(boolean includeSharps) { this.includeSharps = includeSharps; }

    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(int timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }

    public int getMaxFret() { return maxFret; }
    public void setMaxFret(int maxFret) { this.maxFret = maxFret; }

    public boolean isPlayReferenceAudio() { return playReferenceAudio; }
    public void setPlayReferenceAudio(boolean playReferenceAudio) { this.playReferenceAudio = playReferenceAudio; }

    public boolean isMicrophoneEnabled() { return microphoneEnabled; }
    public void setMicrophoneEnabled(boolean microphoneEnabled) { this.microphoneEnabled = microphoneEnabled; }

    public double getMicSensitivity() { return micSensitivity; }
    public void setMicSensitivity(double micSensitivity) { this.micSensitivity = micSensitivity; }

    public boolean isMatchExactOctave() { return matchExactOctave; }
    public void setMatchExactOctave(boolean matchExactOctave) { this.matchExactOctave = matchExactOctave; }

    public double getMinConfidence() { return minConfidence; }
    public void setMinConfidence(double minConfidence) { this.minConfidence = minConfidence; }

    public boolean isAdaptiveLearning() { return adaptiveLearning; }
    public void setAdaptiveLearning(boolean adaptiveLearning) { this.adaptiveLearning = adaptiveLearning; }
}

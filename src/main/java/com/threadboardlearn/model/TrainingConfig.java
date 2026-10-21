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

    public TrainingConfig() {
        this.selectedString = null; // Todas las cuerdas
        this.includeSharps = false; // Solo naturales por defecto
        this.timeLimitSeconds = 5;  // 5 segundos por defecto
        this.maxFret = 12;
        this.playReferenceAudio = true;
        this.microphoneEnabled = true;
        this.micSensitivity = 0.045; // Puerta de ruido para ignorar voces de fondo
        this.matchExactOctave = true; // Exigir octava real de la cuerda
        this.minConfidence = 0.75;
    }

    public boolean isAllStrings() {
        return selectedString == null;
    }
}

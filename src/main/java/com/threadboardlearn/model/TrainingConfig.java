package com.threadboardlearn.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Parámetros de configuración de la sesión de entrenamiento.
 */
@Getter
@Setter
public class TrainingConfig {
    private EnumSet<GuitarString> selectedStrings; // Conjunto de cuerdas activas para la práctica
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
        this.selectedStrings = EnumSet.of(GuitarString.STRING_6); // 6ª Cuerda (grave) por defecto
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

    public Set<GuitarString> getSelectedStrings() {
        return Collections.unmodifiableSet(selectedStrings);
    }

    public List<GuitarString> getActiveStringsList() {
        if (selectedStrings == null || selectedStrings.isEmpty()) {
            return List.of(GuitarString.values());
        }
        return new ArrayList<>(selectedStrings);
    }

    public void setSelectedStrings(Collection<GuitarString> strings) {
        if (strings == null || strings.isEmpty()) {
            this.selectedStrings = EnumSet.allOf(GuitarString.class);
        } else {
            this.selectedStrings = EnumSet.copyOf(strings);
        }
    }

    public boolean isStringSelected(GuitarString string) {
        return string != null && selectedStrings.contains(string);
    }

    /**
     * Activa o desactiva una cuerda específica.
     * Garantiza que siempre quede al menos una cuerda seleccionada.
     */
    public void setStringSelected(GuitarString string, boolean selected) {
        if (string == null) return;
        if (selected) {
            selectedStrings.add(string);
        } else if (selectedStrings.size() > 1) {
            selectedStrings.remove(string);
        }
    }

    /**
     * Alterna el estado de selección de una cuerda específica,
     * manteniendo siempre al menos una cuerda activa.
     */
    public void toggleString(GuitarString string) {
        if (string == null) return;
        setStringSelected(string, !isStringSelected(string));
    }

    public boolean isAllStrings() {
        return selectedStrings.size() == GuitarString.values().length;
    }

    public boolean isSingleString() {
        return selectedStrings.size() == 1;
    }

    public void setAllStrings(boolean all) {
        if (all) {
            this.selectedStrings = EnumSet.allOf(GuitarString.class);
        } else if (isAllStrings() || this.selectedStrings.isEmpty()) {
            this.selectedStrings = EnumSet.of(GuitarString.STRING_6);
        }
    }

    public GuitarString getSelectedString() {
        return selectedStrings.size() == 1 ? selectedStrings.iterator().next() : null;
    }

    public void setSelectedString(GuitarString selectedString) {
        if (selectedString == null) {
            this.selectedStrings = EnumSet.allOf(GuitarString.class);
        } else {
            this.selectedStrings = EnumSet.of(selectedString);
        }
    }

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

package com.threadboardlearn.engine;

import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;

import java.util.List;

/**
 * Reto individual presentado al usuario.
 */
public record Challenge(
        Note targetNote,
        GuitarString targetString,
        List<Integer> validFrets,
        long startTimeMs,
        boolean isFocus,
        double selectionProbability
) {
    public Challenge(Note targetNote, GuitarString targetString, List<Integer> validFrets, long startTimeMs) {
        this(targetNote, targetString, validFrets, startTimeMs, false, 0.0);
    }

    public boolean isValidFret(GuitarString string, int fret) {
        return this.targetString == string && validFrets.contains(fret);
    }

    /**
     * Comprueba si una frecuencia detectada coincide con alguno de los trastes válidos
     * en la cuerda objetivo dentro de una tolerancia en cents.
     * Soporta tanto la frecuencia fundamental (1x) como el segundo armónico (2x),
     * fundamental para cuerdas graves (6ª y 5ª) donde micrófonos de portátiles atenúan los sub-150 Hz.
     */
    public boolean matchesStringFrequency(double frequencyHz, double maxCentsTolerance) {
        if (frequencyHz <= 0 || targetString == null || validFrets.isEmpty()) {
            return false;
        }

        for (int fret : validFrets) {
            double targetFreq = targetString.getFrequencyAtFret(fret);

            // 1. Fundamental exacta (1x)
            double centsDiff = Math.abs(1200.0 * (Math.log(frequencyHz / targetFreq) / Math.log(2.0)));
            if (centsDiff <= maxCentsTolerance) {
                return true;
            }

            // 2. Segundo armónico (2x / octava superior) para cuerdas graves
            double harmonic2Freq = targetFreq * 2.0;
            double centsDiffHarmonic = Math.abs(1200.0 * (Math.log(frequencyHz / harmonic2Freq) / Math.log(2.0)));
            if (centsDiffHarmonic <= maxCentsTolerance) {
                return true;
            }
        }
        return false;
    }
}

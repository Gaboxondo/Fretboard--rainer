package com.threadboardlearn.dsp;

import com.threadboardlearn.model.Note;

/**
 * Resultado de una pasada de detección de tono sobre una ventana de audio.
 */
public record PitchDetectionResult(
        double frequencyHz,
        double probability,
        Note note,
        double centsDeviation,
        double rms,
        boolean isPitched
) {
    public static PitchDetectionResult unpitched(double rms) {
        return new PitchDetectionResult(-1.0, 0.0, null, 0.0, rms, false);
    }
}

package com.threadboardlearn.dsp;

/**
 * Interfaz para algoritmos de detección de tono (Pitch Detection).
 */
public interface PitchDetector {
    PitchDetectionResult detect(float[] audioBuffer, float sampleRate);
}

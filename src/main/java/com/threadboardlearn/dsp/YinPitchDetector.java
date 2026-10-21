package com.threadboardlearn.dsp;

import com.threadboardlearn.model.Note;

/**
 * Implementación del algoritmo YIN para detección de tono monofónico en tiempo real.
 * Altamente preciso y resistente a errores de salto de octava en el rango de guitarra.
 */
public class YinPitchDetector implements PitchDetector {
    private final double threshold;
    private final double minFrequency;
    private final double maxFrequency;

    public YinPitchDetector() {
        this(0.18, 65.0, 1200.0);
    }

    public YinPitchDetector(double threshold, double minFrequency, double maxFrequency) {
        this.threshold = threshold;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
    }

    @Override
    public PitchDetectionResult detect(float[] audioBuffer, float sampleRate) {
        int bufferSize = audioBuffer.length;
        if (bufferSize < 512) {
            return PitchDetectionResult.unpitched(0.0);
        }

        // 1. Calcular RMS (energía acústica)
        double sumSquares = 0.0;
        for (float sample : audioBuffer) {
            sumSquares += sample * sample;
        }
        double rms = Math.sqrt(sumSquares / bufferSize);
        if (rms < 0.015) { // Umbral de silencio / ruido de fondo
            return PitchDetectionResult.unpitched(rms);
        }

        int halfBufferSize = bufferSize / 2;
        int minTau = (int) Math.floor(sampleRate / maxFrequency);
        int maxTau = Math.min(halfBufferSize - 1, (int) Math.ceil(sampleRate / minFrequency));

        if (maxTau <= minTau) {
            return PitchDetectionResult.unpitched(rms);
        }

        // 2. Función de diferencia YIN
        double[] yinBuffer = new double[halfBufferSize];
        for (int tau = minTau; tau <= maxTau; tau++) {
            double diff = 0.0;
            for (int j = 0; j < halfBufferSize; j++) {
                double delta = audioBuffer[j] - audioBuffer[j + tau];
                diff += delta * delta;
            }
            yinBuffer[tau] = diff;
        }

        // 3. Normalización acumulativa por media
        yinBuffer[0] = 1.0;
        double runningSum = 0.0;
        for (int tau = 1; tau <= maxTau; tau++) {
            runningSum += yinBuffer[tau];
            if (runningSum > 0.0) {
                yinBuffer[tau] *= tau / runningSum;
            } else {
                yinBuffer[tau] = 1.0;
            }
        }

        // 4. Búsqueda de caída por debajo del umbral absoluto
        int tauEstimate = -1;
        for (int tau = minTau; tau <= maxTau; tau++) {
            if (yinBuffer[tau] < threshold) {
                // Seguir descendiendo hasta el mínimo local
                while (tau + 1 <= maxTau && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++;
                }
                tauEstimate = tau;
                break;
            }
        }

        // Si no bajó del umbral, buscar el mínimo global absoluto si es aceptable
        if (tauEstimate == -1) {
            double globalMin = Double.MAX_VALUE;
            int bestTau = -1;
            for (int tau = minTau; tau <= maxTau; tau++) {
                if (yinBuffer[tau] < globalMin) {
                    globalMin = yinBuffer[tau];
                    bestTau = tau;
                }
            }
            if (globalMin < 0.35 && bestTau != -1) {
                tauEstimate = bestTau;
            }
        }

        if (tauEstimate == -1 || tauEstimate <= 0) {
            return PitchDetectionResult.unpitched(rms);
        }

        // 5. Interpolación parabólica para precisión de sub-muestra
        double betterTau;
        int x0 = (tauEstimate < 1) ? tauEstimate : tauEstimate - 1;
        int x2 = (tauEstimate + 1 < halfBufferSize) ? tauEstimate + 1 : tauEstimate;

        if (x0 == tauEstimate) {
            betterTau = (yinBuffer[tauEstimate] <= yinBuffer[x2]) ? tauEstimate : x2;
        } else if (x2 == tauEstimate) {
            betterTau = (yinBuffer[tauEstimate] <= yinBuffer[x0]) ? tauEstimate : x0;
        } else {
            double s0 = yinBuffer[x0];
            double s1 = yinBuffer[tauEstimate];
            double s2 = yinBuffer[x2];
            double denominator = 2.0 * (2.0 * s1 - s2 - s0);
            if (Math.abs(denominator) > 1e-6) {
                betterTau = tauEstimate + (s2 - s0) / denominator;
            } else {
                betterTau = tauEstimate;
            }
        }

        double pitchInHz = sampleRate / betterTau;
        if (pitchInHz < minFrequency || pitchInHz > maxFrequency) {
            return PitchDetectionResult.unpitched(rms);
        }

        double confidence = Math.max(0.0, 1.0 - yinBuffer[tauEstimate]);
        Note detectedNote = Note.fromFrequency(pitchInHz);
        double cents = Note.centsDeviation(pitchInHz);

        return new PitchDetectionResult(pitchInHz, confidence, detectedNote, cents, rms, true);
    }
}

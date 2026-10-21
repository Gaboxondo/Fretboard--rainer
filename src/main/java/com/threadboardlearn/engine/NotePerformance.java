package com.threadboardlearn.engine;

import com.threadboardlearn.model.Note;
import lombok.Getter;

/**
 * Registro de rendimiento y métricas de aprendizaje para una nota específica.
 * Calcula el peso dinámico de probabilidad de la nota en base a fallos,
 * tiempos de reacción y respuestas correctas consecutivas.
 */
public class NotePerformance {
    @Getter
    private final Note note;
    @Getter
    private int attempts = 0;
    @Getter
    private int successes = 0;
    @Getter
    private int mistakes = 0;
    @Getter
    private int timeouts = 0;
    @Getter
    private long totalReactionTimeMs = 0;
    @Getter
    private double averageReactionTimeMs = 0.0;
    @Getter
    private double errorScore = 0.0;
    @Getter
    private int consecutiveSuccesses = 0;

    public NotePerformance(Note note) {
        this.note = note;
    }

    /**
     * Registra un acierto con su tiempo de reacción.
     * Si el acierto es rápido, el errorScore decae rápidamente premiando el dominio.
     */
    public synchronized void recordSuccess(long reactionTimeMs) {
        attempts++;
        successes++;
        consecutiveSuccesses++;
        totalReactionTimeMs += reactionTimeMs;

        if (successes == 1) {
            averageReactionTimeMs = reactionTimeMs;
        } else {
            // Media móvil ponderada hacia los intentos más recientes (alpha = 0.6)
            averageReactionTimeMs = 0.6 * reactionTimeMs + 0.4 * averageReactionTimeMs;
        }

        // Decaimiento del errorScore según velocidad de resolución
        if (reactionTimeMs < 2000) {
            errorScore = Math.max(0.0, errorScore * 0.5 - 0.5);
        } else if (reactionTimeMs < 4000) {
            errorScore = Math.max(0.0, errorScore * 0.75 - 0.2);
        } else {
            // Acierto lento: el error decae poco porque aún no se domina con soltura
            errorScore = Math.max(0.0, errorScore * 0.9);
        }
    }

    /**
     * Registra un clic erróneo mientras se buscaba esta nota.
     */
    public synchronized void recordMistake() {
        mistakes++;
        consecutiveSuccesses = 0;
        errorScore += 1.0;
    }

    /**
     * Registra que el tiempo expiró sin encontrar esta nota.
     */
    public synchronized void recordTimeout() {
        attempts++;
        timeouts++;
        consecutiveSuccesses = 0;
        errorScore += 2.5;
    }

    /**
     * Calcula el peso de muestreo probabilístico para esta nota.
     * Mínimo peso base = 1.0.
     * Aumenta por:
     * - errorScore acumulado (fallos y timeouts).
     * - lentitud en encontrar la nota (reactionTime medio > 1.5s).
     */
    public synchronized double getWeight() {
        double weight = 1.0;

        // 1. Penalización acumulada por errores o tiempo agotado
        weight += errorScore;

        // 2. Penalización progresiva por lentitud (si supera 1.5s)
        if (averageReactionTimeMs > 1500.0) {
            double extraSeconds = (averageReactionTimeMs - 1500.0) / 1000.0;
            double timeWeight = Math.min(8.0, extraSeconds * 0.8);
            weight += timeWeight;
        }

        return Math.max(1.0, weight);
    }

    public synchronized double getAccuracyPercent() {
        int total = successes + timeouts;
        if (total == 0) return 0.0;
        return (successes * 100.0) / total;
    }

    public synchronized double getAverageReactionTimeSeconds() {
        return averageReactionTimeMs / 1000.0;
    }

    public synchronized void reset() {
        attempts = 0;
        successes = 0;
        mistakes = 0;
        timeouts = 0;
        totalReactionTimeMs = 0;
        averageReactionTimeMs = 0.0;
        errorScore = 0.0;
        consecutiveSuccesses = 0;
    }
}

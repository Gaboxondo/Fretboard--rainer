package com.threadboardlearn.engine;

import com.threadboardlearn.model.GuitarString;
import lombok.Getter;

/**
 * Registro de rendimiento y fallos acumulados para una cuerda de guitarra específica.
 */
public class StringPerformance {
    @Getter
    private final GuitarString string;
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

    public StringPerformance(GuitarString string) {
        this.string = string;
    }

    public synchronized void recordSuccess(long reactionTimeMs) {
        attempts++;
        successes++;
        totalReactionTimeMs += reactionTimeMs;
        if (successes == 1) {
            averageReactionTimeMs = reactionTimeMs;
        } else {
            averageReactionTimeMs = 0.6 * reactionTimeMs + 0.4 * averageReactionTimeMs;
        }
    }

    public synchronized void recordMistake() {
        mistakes++;
    }

    public synchronized void recordTimeout() {
        attempts++;
        timeouts++;
    }

    public synchronized int getTotalFailures() {
        return mistakes + timeouts;
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
    }
}

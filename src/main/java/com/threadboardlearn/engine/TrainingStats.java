package com.threadboardlearn.engine;

import lombok.Getter;

/**
 * Estadísticas de rendimiento de la sesión actual de entrenamiento.
 */
@Getter
public class TrainingStats {
    private int totalRounds = 0;
    private int correctCount = 0;
    private int timeoutCount = 0;
    private int currentStreak = 0;
    private int bestStreak = 0;
    private long totalReactionTimeMs = 0;

    public synchronized void recordSuccess(long reactionTimeMs) {
        totalRounds++;
        correctCount++;
        currentStreak++;
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }
        totalReactionTimeMs += reactionTimeMs;
    }

    public synchronized void recordTimeout() {
        totalRounds++;
        timeoutCount++;
        currentStreak = 0;
    }

    public synchronized void reset() {
        totalRounds = 0;
        correctCount = 0;
        timeoutCount = 0;
        currentStreak = 0;
        bestStreak = 0;
        totalReactionTimeMs = 0;
    }

    public double getAccuracyPercent() {
        if (totalRounds == 0) return 0.0;
        return (correctCount * 100.0) / totalRounds;
    }

    public double getAverageReactionTimeSeconds() {
        if (correctCount == 0) return 0.0;
        return (totalReactionTimeMs / 1000.0) / correctCount;
    }
}

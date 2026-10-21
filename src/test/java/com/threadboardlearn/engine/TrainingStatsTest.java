package com.threadboardlearn.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TrainingStatsTest {

    @Test
    @DisplayName("Debe registrar aciertos, rachas y cálculo de tiempo medio")
    void testRecordSuccess() {
        TrainingStats stats = new TrainingStats();
        assertThat(stats.getTotalRounds()).isEqualTo(0);
        assertThat(stats.getCurrentStreak()).isEqualTo(0);

        stats.recordSuccess(1000);
        stats.recordSuccess(2000);

        assertThat(stats.getTotalRounds()).isEqualTo(2);
        assertThat(stats.getCorrectCount()).isEqualTo(2);
        assertThat(stats.getCurrentStreak()).isEqualTo(2);
        assertThat(stats.getBestStreak()).isEqualTo(2);
        assertThat(stats.getAverageReactionTimeSeconds()).isCloseTo(1.5, within(0.001));
        assertThat(stats.getAccuracyPercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Un fallo o clic incorrecto debe resetear la racha actual e incrementar el contador de fallos")
    void testRecordMistake() {
        TrainingStats stats = new TrainingStats();
        stats.recordSuccess(1000);
        stats.recordSuccess(1200);
        assertThat(stats.getCurrentStreak()).isEqualTo(2);

        stats.recordMistake();

        assertThat(stats.getCurrentStreak()).isEqualTo(0);
        assertThat(stats.getBestStreak()).isEqualTo(2);
        assertThat(stats.getMistakeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("El tiempo agotado resetea la racha e incrementa timeoutCount y totalRounds")
    void testRecordTimeout() {
        TrainingStats stats = new TrainingStats();
        stats.recordSuccess(1000);
        assertThat(stats.getCurrentStreak()).isEqualTo(1);

        stats.recordTimeout();

        assertThat(stats.getCurrentStreak()).isEqualTo(0);
        assertThat(stats.getTimeoutCount()).isEqualTo(1);
        assertThat(stats.getTotalRounds()).isEqualTo(2);
        assertThat(stats.getAccuracyPercent()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Reset debe reiniciar todos los contadores a 0")
    void testReset() {
        TrainingStats stats = new TrainingStats();
        stats.recordSuccess(1000);
        stats.recordMistake();
        stats.recordTimeout();

        stats.reset();

        assertThat(stats.getTotalRounds()).isEqualTo(0);
        assertThat(stats.getCorrectCount()).isEqualTo(0);
        assertThat(stats.getMistakeCount()).isEqualTo(0);
        assertThat(stats.getTimeoutCount()).isEqualTo(0);
        assertThat(stats.getCurrentStreak()).isEqualTo(0);
        assertThat(stats.getBestStreak()).isEqualTo(0);
        assertThat(stats.getAccuracyPercent()).isEqualTo(0.0);
    }
}

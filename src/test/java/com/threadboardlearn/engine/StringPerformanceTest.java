package com.threadboardlearn.engine;

import com.threadboardlearn.model.GuitarString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StringPerformanceTest {

    @Test
    @DisplayName("Debe registrar éxitos, errores, tiempos de reacción y calcular precisión por cuerda")
    void testRecordSuccessAndFailures() {
        StringPerformance sp = new StringPerformance(GuitarString.STRING_6);
        assertThat(sp.getString()).isEqualTo(GuitarString.STRING_6);
        assertThat(sp.getTotalFailures()).isEqualTo(0);

        sp.recordSuccess(1000);
        sp.recordSuccess(1400);
        sp.recordMistake();
        sp.recordTimeout();

        assertThat(sp.getSuccesses()).isEqualTo(2);
        assertThat(sp.getMistakes()).isEqualTo(1);
        assertThat(sp.getTimeouts()).isEqualTo(1);
        assertThat(sp.getTotalFailures()).isEqualTo(2);
        assertThat(sp.getAttempts()).isEqualTo(3); // 2 successes + 1 timeout
        assertThat(sp.getAccuracyPercent()).isCloseTo(66.666, within(0.01));
        assertThat(sp.getAverageReactionTimeSeconds()).isCloseTo(1.24, within(0.01));
    }

    @Test
    @DisplayName("Reset debe poner a cero todas las métricas de la cuerda")
    void testReset() {
        StringPerformance sp = new StringPerformance(GuitarString.STRING_1);
        sp.recordSuccess(1500);
        sp.recordMistake();
        sp.recordTimeout();

        sp.reset();

        assertThat(sp.getAttempts()).isEqualTo(0);
        assertThat(sp.getSuccesses()).isEqualTo(0);
        assertThat(sp.getMistakes()).isEqualTo(0);
        assertThat(sp.getTimeouts()).isEqualTo(0);
        assertThat(sp.getTotalFailures()).isEqualTo(0);
        assertThat(sp.getAccuracyPercent()).isEqualTo(0.0);
        assertThat(sp.getAverageReactionTimeSeconds()).isEqualTo(0.0);
    }
}

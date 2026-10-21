package com.threadboardlearn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingConfigTest {

    @Test
    @DisplayName("La configuración por defecto debe tener la 6ª cuerda (grave) y 3 segundos seleccionados")
    void testDefaultValues() {
        TrainingConfig config = new TrainingConfig();

        assertThat(config.getSelectedString()).isEqualTo(GuitarString.STRING_6);
        assertThat(config.getTimeLimitSeconds()).isEqualTo(3);
        assertThat(config.isAllStrings()).isFalse();
        assertThat(config.isIncludeSharps()).isFalse();
        assertThat(config.getMaxFret()).isEqualTo(12);
        assertThat(config.isAdaptiveLearning()).isTrue();
    }

    @Test
    @DisplayName("isAllStrings debe retornar true solo si selectedString es null")
    void testAllStringsFlag() {
        TrainingConfig config = new TrainingConfig();
        assertThat(config.isAllStrings()).isFalse();

        config.setSelectedString(null);
        assertThat(config.isAllStrings()).isTrue();

        config.setSelectedString(GuitarString.STRING_1);
        assertThat(config.isAllStrings()).isFalse();
    }
}

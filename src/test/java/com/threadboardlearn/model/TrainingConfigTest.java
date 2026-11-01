package com.threadboardlearn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingConfigTest {

    @Test
    @DisplayName("La configuración por defecto debe tener la 6ª cuerda (grave) y 3 segundos seleccionados")
    void testDefaultValues() {
        TrainingConfig config = new TrainingConfig();

        assertThat(config.getSelectedString()).isEqualTo(GuitarString.STRING_6);
        assertThat(config.getSelectedStrings()).containsExactly(GuitarString.STRING_6);
        assertThat(config.getTimeLimitSeconds()).isEqualTo(3);
        assertThat(config.isAllStrings()).isFalse();
        assertThat(config.isSingleString()).isTrue();
        assertThat(config.isIncludeSharps()).isFalse();
        assertThat(config.getMaxFret()).isEqualTo(12);
        assertThat(config.isAdaptiveLearning()).isTrue();
    }

    @Test
    @DisplayName("isAllStrings debe retornar true cuando están seleccionadas las 6 cuerdas")
    void testAllStringsFlag() {
        TrainingConfig config = new TrainingConfig();
        assertThat(config.isAllStrings()).isFalse();

        config.setSelectedString(null);
        assertThat(config.isAllStrings()).isTrue();
        assertThat(config.getSelectedStrings()).hasSize(6);

        config.setSelectedString(GuitarString.STRING_1);
        assertThat(config.isAllStrings()).isFalse();
        assertThat(config.isSingleString()).isTrue();
    }

    @Test
    @DisplayName("Debe permitir seleccionar múltiples cuerdas específicas como 6ª y 5ª cuerda")
    void testMultiStringSelection() {
        TrainingConfig config = new TrainingConfig(); // Inicia con STRING_6
        config.toggleString(GuitarString.STRING_5);

        assertThat(config.getSelectedStrings())
                .containsExactlyInAnyOrder(GuitarString.STRING_6, GuitarString.STRING_5);
        assertThat(config.isAllStrings()).isFalse();
        assertThat(config.isSingleString()).isFalse();
        assertThat(config.getSelectedString()).isNull();

        // No debe permitir dejar 0 cuerdas activas al intentar desactivar la última
        config.setSelectedStrings(Set.of(GuitarString.STRING_6));
        config.toggleString(GuitarString.STRING_6);
        assertThat(config.getSelectedStrings()).containsExactly(GuitarString.STRING_6);
    }
}

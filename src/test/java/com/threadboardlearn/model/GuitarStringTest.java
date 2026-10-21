package com.threadboardlearn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuitarStringTest {

    @Test
    @DisplayName("Las 6 cuerdas deben tener la afinación estándar correcta al aire")
    void testStandardTuningOpenNotes() {
        assertThat(GuitarString.STRING_1.getOpenNote()).isEqualTo(Note.E); // E4
        assertThat(GuitarString.STRING_2.getOpenNote()).isEqualTo(Note.B); // B3
        assertThat(GuitarString.STRING_3.getOpenNote()).isEqualTo(Note.G); // G3
        assertThat(GuitarString.STRING_4.getOpenNote()).isEqualTo(Note.D); // D3
        assertThat(GuitarString.STRING_5.getOpenNote()).isEqualTo(Note.A); // A2
        assertThat(GuitarString.STRING_6.getOpenNote()).isEqualTo(Note.E); // E2
    }

    @Test
    @DisplayName("El traste 12 debe ser la misma nota que la cuerda al aire (octava superior)")
    void testTwelfthFretOctave() {
        for (GuitarString string : GuitarString.values()) {
            assertThat(string.getNoteAtFret(12))
                    .as("El traste 12 de " + string.getLabel() + " debe ser " + string.getOpenNote())
                    .isEqualTo(string.getOpenNote());
        }
    }

    @Test
    @DisplayName("El traste 5 de la 6ª cuerda (E) debe ser la nota A (La)")
    void testFifthFretLowE() {
        assertThat(GuitarString.STRING_6.getNoteAtFret(5)).isEqualTo(Note.A);
    }

    @Test
    @DisplayName("El traste 3 de la 5ª cuerda (A) debe ser la nota C (Do)")
    void testThirdFretAString() {
        assertThat(GuitarString.STRING_5.getNoteAtFret(3)).isEqualTo(Note.C);
    }

    @Test
    @DisplayName("Debe encontrar todos los trastes donde reside una nota en una cuerda")
    void testFindFretsForNote() {
        // En la 6ª cuerda (E), la nota E está en el traste 0 y en el traste 12
        List<Integer> fretsForE = GuitarString.STRING_6.findFretsForNote(Note.E, 12);
        assertThat(fretsForE).containsExactly(0, 12);

        // En la 6ª cuerda, la nota G está en el traste 3 (E -> F en 1, F# en 2, G en 3)
        List<Integer> fretsForG = GuitarString.STRING_6.findFretsForNote(Note.G, 12);
        assertThat(fretsForG).containsExactly(3);
    }
}

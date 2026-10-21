package com.threadboardlearn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoteTest {

    @Test
    @DisplayName("Debe tener 7 notas naturales (A, B, C, D, E, F, G)")
    void testNaturalNotes() {
        List<Note> naturals = Note.getNaturalNotes();
        assertThat(naturals).hasSize(7);
        assertThat(naturals.stream().map(Note::getName).toList())
                .containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G");
    }

    @Test
    @DisplayName("B y E no deben tener sostenidos (sharps)")
    void testNoSharpsForBandE() {
        // En nuestro modelo, todos los sostenidos son A#, C#, D#, F#, G#
        List<Note> sharps = Note.getAllNotes().stream().filter(Note::isSharp).toList();
        assertThat(sharps).hasSize(5);
        assertThat(sharps.stream().map(Note::getName).toList())
                .containsExactlyInAnyOrder("A#", "C#", "D#", "F#", "G#");

        assertThat(Note.B.isSharp()).isFalse();
        assertThat(Note.E.isSharp()).isFalse();
    }

    @Test
    @DisplayName("Debe identificar correctamente la nota a partir de frecuencias de referencia")
    void testFromFrequency() {
        // A4 = 440 Hz
        assertThat(Note.fromFrequency(440.0)).isEqualTo(Note.A);
        // A2 = 110 Hz (5ª cuerda al aire)
        assertThat(Note.fromFrequency(110.0)).isEqualTo(Note.A);
        // E2 = ~82.41 Hz (6ª cuerda al aire)
        assertThat(Note.fromFrequency(82.41)).isEqualTo(Note.E);
        // E4 = ~329.63 Hz (1ª cuerda al aire)
        assertThat(Note.fromFrequency(329.63)).isEqualTo(Note.E);
        // C4 = ~261.63 Hz
        assertThat(Note.fromFrequency(261.63)).isEqualTo(Note.C);
        // F#3 = ~185.0 Hz
        assertThat(Note.fromFrequency(185.0)).isEqualTo(Note.F_SHARP);
    }
}

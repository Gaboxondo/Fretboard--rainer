package com.threadboardlearn.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Representa las notas musicales del sistema cromático estándar con notación anglosajona.
 * Notas naturales: A, B, C, D, E, F, G
 * Sostenidos: A#, C#, D#, F#, G# (B y E no tienen sostenido).
 */
@Getter
public enum Note {
    C("C", 0, false),
    C_SHARP("C#", 1, true),
    D("D", 2, false),
    D_SHARP("D#", 3, true),
    E("E", 4, false),
    F("F", 5, false),
    F_SHARP("F#", 6, true),
    G("G", 7, false),
    G_SHARP("G#", 8, true),
    A("A", 9, false),
    A_SHARP("A#", 10, true),
    B("B", 11, false);

    private final String name;
    private final int semitone; // Relativo a C (0..11)
    private final boolean sharp;

    Note(String name, int semitone, boolean sharp) {
        this.name = name;
        this.semitone = semitone;
        this.sharp = sharp;
    }

    public String getName() {
        return name;
    }

    public int getSemitone() {
        return semitone;
    }

    public boolean isSharp() {
        return sharp;
    }

    public static List<Note> getNaturalNotes() {
        return Arrays.stream(values())
                .filter(n -> !n.isSharp())
                .toList();
    }

    public static List<Note> getAllNotes() {
        return Arrays.asList(values());
    }

    /**
     * Obtiene la nota a partir del índice de semitono (0 = C ... 11 = B).
     */
    public static Note fromSemitone(int semitone) {
        int index = Math.floorMod(semitone, 12);
        for (Note n : values()) {
            if (n.semitone == index) {
                return n;
            }
        }
        return C;
    }

    /**
     * Obtiene la nota más cercana a una frecuencia en Hz.
     */
    public static Note fromFrequency(double frequencyHz) {
        if (frequencyHz <= 0) return null;
        double midi = 69.0 + 12.0 * (Math.log(frequencyHz / 440.0) / Math.log(2.0));
        int roundedMidi = (int) Math.round(midi);
        return fromSemitone(roundedMidi);
    }

    /**
     * Calcula la desviación en cents (-50 a +50) respecto a la frecuencia dada.
     */
    public static double centsDeviation(double frequencyHz) {
        if (frequencyHz <= 0) return 0.0;
        double midi = 69.0 + 12.0 * (Math.log(frequencyHz / 440.0) / Math.log(2.0));
        double nearestMidi = Math.round(midi);
        return (midi - nearestMidi) * 100.0;
    }

    @Override
    public String toString() {
        return name;
    }
}

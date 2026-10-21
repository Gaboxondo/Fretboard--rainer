package com.threadboardlearn.model;

/**
 * Representa una posición específica en el diapasón (cuerda y traste).
 */
public record FretboardPosition(GuitarString string, int fret, Note note, double frequencyHz) {
    public static FretboardPosition of(GuitarString string, int fret) {
        return new FretboardPosition(
                string,
                fret,
                string.getNoteAtFret(fret),
                string.getFrequencyAtFret(fret)
        );
    }
}

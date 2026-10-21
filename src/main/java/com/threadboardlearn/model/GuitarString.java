package com.threadboardlearn.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una de las 6 cuerdas de la guitarra en afinación estándar.
 * Número de cuerda 1 (más aguda, High E) a 6 (más grave, Low E).
 */
@Getter
public enum GuitarString {
    STRING_1(1, "1ª Cuerda (E4 / Aguda)", Note.E, 64, 1.0f),
    STRING_2(2, "2ª Cuerda (B3)", Note.B, 59, 1.4f),
    STRING_3(3, "3ª Cuerda (G3)", Note.G, 55, 1.9f),
    STRING_4(4, "4ª Cuerda (D3)", Note.D, 50, 2.5f),
    STRING_5(5, "5ª Cuerda (A2)", Note.A, 45, 3.2f),
    STRING_6(6, "6ª Cuerda (E2 / Grave)", Note.E, 40, 4.0f);

    private final int number;
    private final String label;
    private final Note openNote;
    private final int openMidiNote;
    private final float visualGauge; // Grosor visual relativo para el dibujo

    GuitarString(int number, String label, Note openNote, int openMidiNote, float visualGauge) {
        this.number = number;
        this.label = label;
        this.openNote = openNote;
        this.openMidiNote = openMidiNote;
        this.visualGauge = visualGauge;
    }

    /**
     * Obtiene la etiqueta de la cuerda traducida al idioma activo.
     */
    public String getLocalizedLabel() {
        return com.threadboardlearn.i18n.I18n.get("string." + number);
    }

    /**
     * Obtiene la nota en un traste dado (0 = cuerda al aire).
     */
    public Note getNoteAtFret(int fret) {
        return Note.fromSemitone(openMidiNote + fret);
    }

    /**
     * Obtiene el valor MIDI en un traste dado.
     */
    public int getMidiNoteAtFret(int fret) {
        return openMidiNote + fret;
    }

    /**
     * Obtiene la frecuencia en Hertz para un traste dado.
     */
    public double getFrequencyAtFret(int fret) {
        int midi = openMidiNote + fret;
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    /**
     * Encuentra todos los trastes en esta cuerda (dentro de maxFret) que contienen la nota especificada.
     */
    public List<Integer> findFretsForNote(Note targetNote, int maxFret) {
        List<Integer> frets = new ArrayList<>();
        for (int fret = 0; fret <= maxFret; fret++) {
            if (getNoteAtFret(fret) == targetNote) {
                frets.add(fret);
            }
        }
        return frets;
    }

    /**
     * Busca la cuerda por su número (1 a 6).
     */
    public static GuitarString fromNumber(int number) {
        for (GuitarString s : values()) {
            if (s.number == number) return s;
        }
        return STRING_1;
    }
}

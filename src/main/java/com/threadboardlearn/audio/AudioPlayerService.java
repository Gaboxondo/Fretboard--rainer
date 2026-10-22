package com.threadboardlearn.audio;

import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;

/**
 * Servicio abstracto de reproducción de audio.
 * Permite implementaciones en Desktop (Java MIDI / Sound) y en Android (SoundPool / AudioTrack).
 */
public interface AudioPlayerService {
    void playGuitarNote(GuitarString string, int fret);
    void playNote(Note note, GuitarString preferredString);
    void playSuccessSound();
    void playFailureSound();
    void close();
}

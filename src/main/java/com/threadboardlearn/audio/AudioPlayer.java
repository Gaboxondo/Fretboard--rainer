package com.threadboardlearn.audio;

import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reproductor de audio para notas de guitarra y efectos de feedback (acierto/fallo).
 * Utiliza el sintetizador MIDI nativo de Java con instrumento de Guitarra Acústica,
 * con fallback a síntesis por onda de audio PCM si el sintetizador MIDI no estuviera disponible.
 */
public class AudioPlayer {
    private static final int GUITAR_STEEL_PROGRAM = 25; // General MIDI 25: Acoustic Guitar (Steel)
    private static final int CHIME_PROGRAM = 10;        // Music Box / Bell for correct
    private static final int GUITAR_CHANNEL = 0;

    private Synthesizer synthesizer;
    private MidiChannel guitarChannel;
    private boolean midiAvailable = false;
    private final ExecutorService soundExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public AudioPlayer() {
        initMidi();
    }

    private void initMidi() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] channels = synthesizer.getChannels();
            if (channels != null && channels.length > 0) {
                guitarChannel = channels[GUITAR_CHANNEL];
                // Programar instrumento de guitarra acústica
                guitarChannel.programChange(GUITAR_STEEL_PROGRAM);
                midiAvailable = true;
            }
        } catch (Exception e) {
            System.err.println("Aviso: Sintetizador MIDI no disponible. Usando fallback PCM: " + e.getMessage());
            midiAvailable = false;
        }
    }

    /**
     * Reproduce una nota en una cuerda y traste específicos con timbre de guitarra.
     */
    public void playGuitarNote(GuitarString string, int fret) {
        int midiNote = string.getMidiNoteAtFret(fret);
        playMidiNote(midiNote, 100, 1500);
    }

    /**
     * Reproduce una nota objetivo en un registro audible adecuado para guitarra.
     */
    public void playNote(Note note, GuitarString preferredString) {
        int midiNote;
        if (preferredString != null) {
            // Buscar el traste más cercano en la cuerda seleccionada
            int fret = preferredString.findFretsForNote(note, 12).stream().findFirst().orElse(0);
            midiNote = preferredString.getMidiNoteAtFret(fret);
        } else {
            // Registro medio de guitarra (Octava 3 o 4)
            midiNote = 48 + note.getSemitone(); // C3 = 48, A3 = 57, etc.
            if (midiNote < 40) midiNote += 12;
        }
        playMidiNote(midiNote, 95, 1200);
    }

    private void playMidiNote(int midiNote, int velocity, int durationMs) {
        soundExecutor.submit(() -> {
            if (midiAvailable && guitarChannel != null) {
                try {
                    guitarChannel.noteOn(midiNote, velocity);
                    Thread.sleep(durationMs);
                    guitarChannel.noteOff(midiNote);
                } catch (Exception e) {
                    playPcmTone(midiToFreq(midiNote), durationMs);
                }
            } else {
                playPcmTone(midiToFreq(midiNote), durationMs);
            }
        });
    }

    /**
     * Sonido de éxito cuando el usuario acierta la nota.
     */
    public void playSuccessSound() {
        soundExecutor.submit(() -> {
            if (midiAvailable && guitarChannel != null) {
                try {
                    // Acorde rápido alegre arpegiado (C - E - G)
                    guitarChannel.noteOn(72, 110);
                    Thread.sleep(80);
                    guitarChannel.noteOn(76, 115);
                    Thread.sleep(80);
                    guitarChannel.noteOn(79, 120);
                    Thread.sleep(350);
                    guitarChannel.allNotesOff();
                } catch (Exception e) {
                    playPcmTone(880, 200);
                }
            } else {
                playPcmTone(880, 200);
            }
        });
    }

    /**
     * Sonido de tiempo agotado o error.
     */
    public void playFailureSound() {
        soundExecutor.submit(() -> {
            if (midiAvailable && guitarChannel != null) {
                try {
                    guitarChannel.noteOn(43, 90); // G2 grave
                    Thread.sleep(120);
                    guitarChannel.noteOn(42, 90); // F#2 más grave
                    Thread.sleep(250);
                    guitarChannel.allNotesOff();
                } catch (Exception e) {
                    playPcmTone(150, 250);
                }
            } else {
                playPcmTone(150, 250);
            }
        });
    }

    private double midiToFreq(int midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    /**
     * Fallback de síntesis de tono senoidal con armónicos y decaimiento.
     */
    private void playPcmTone(double frequencyHz, int durationMs) {
        try {
            float sampleRate = 44100f;
            int numSamples = (int) (sampleRate * (durationMs / 1000.0));
            byte[] buffer = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double time = i / sampleRate;
                // Envolvente de decaimiento exponencial (tipo cuerda pulsada)
                double decay = Math.exp(-3.5 * (double) i / numSamples);
                // Fundamental + segundo y tercer armónico
                double sample = 0.7 * Math.sin(2.0 * Math.PI * frequencyHz * time)
                        + 0.2 * Math.sin(4.0 * Math.PI * frequencyHz * time)
                        + 0.1 * Math.sin(6.0 * Math.PI * frequencyHz * time);
                sample *= decay;

                short shortVal = (short) (sample * 28000);
                buffer[2 * i] = (byte) (shortVal & 0xFF);
                buffer[2 * i + 1] = (byte) ((shortVal >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception ignored) {
        }
    }

    public void close() {
        if (synthesizer != null && synthesizer.isOpen()) {
            synthesizer.close();
        }
        soundExecutor.shutdown();
    }
}

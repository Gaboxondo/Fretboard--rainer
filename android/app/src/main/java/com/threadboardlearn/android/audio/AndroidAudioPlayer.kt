package com.threadboardlearn.android.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.threadboardlearn.audio.AudioPlayerService
import com.threadboardlearn.model.GuitarString
import com.threadboardlearn.model.Note
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Implementación de AudioPlayerService para Android utilizando AudioTrack nativo
 * y síntesis armónica con envolvente de decaimiento (simulación de cuerda pulsada).
 */
class AndroidAudioPlayer : AudioPlayerService {

    private val soundExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val sampleRate = 44100

    override fun playGuitarNote(string: GuitarString, fret: Int) {
        val midiNote = string.getMidiNoteAtFret(fret)
        val freq = midiToFreq(midiNote)
        playPcmTone(freq, 1200)
    }

    override fun playNote(note: Note, preferredString: GuitarString?) {
        val midiNote = if (preferredString != null) {
            val fret = preferredString.findFretsForNote(note, 12).firstOrNull() ?: 0
            preferredString.getMidiNoteAtFret(fret)
        } else {
            var mid = 48 + note.semitone
            if (mid < 40) mid += 12
            mid
        }
        playPcmTone(midiToFreq(midiNote), 1000)
    }

    override fun playSuccessSound() {
        soundExecutor.submit {
            // Arpegio mayor alegre rápido (Do - Mi - Sol)
            val notes = listOf(523.25, 659.25, 783.99)
            for (f in notes) {
                generateAndPlayTone(f, 90, 0.4)
                try { Thread.sleep(70) } catch (_: Exception) {}
            }
        }
    }

    override fun playFailureSound() {
        soundExecutor.submit {
            // Dos tonos descendentes
            generateAndPlayTone(196.0, 100, 0.5) // G3
            try { Thread.sleep(80) } catch (_: Exception) {}
            generateAndPlayTone(164.81, 160, 0.5) // E3
        }
    }

    override fun close() {
        soundExecutor.shutdown()
    }

    private fun playPcmTone(frequencyHz: Double, durationMs: Int) {
        soundExecutor.submit {
            generateAndPlayTone(frequencyHz, durationMs, 0.8)
        }
    }

    private fun generateAndPlayTone(frequencyHz: Double, durationMs: Int, volume: Double) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val decay = exp(-3.2 * i.toDouble() / numSamples)
                // Fundamental + segundo y tercer armónico de cuerda de guitarra
                val sample = (0.7 * sin(2.0 * PI * frequencyHz * time)
                        + 0.2 * sin(4.0 * PI * frequencyHz * time)
                        + 0.1 * sin(6.0 * PI * frequencyHz * time)) * decay * volume

                samples[i] = (sample * 28000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Esperar que termine de reproducir para liberar recursos
            Thread.sleep(durationMs.toLong() + 50)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
        }
    }

    private fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69.0) / 12.0)
}

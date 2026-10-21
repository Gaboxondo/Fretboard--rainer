package com.threadboardlearn.dsp;

import com.threadboardlearn.model.Note;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class YinPitchDetectorTest {

    private final YinPitchDetector detector = new YinPitchDetector();
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int BUFFER_SIZE = 2048;

    @Test
    @DisplayName("Debe detectar tono puro de 110 Hz (A2 - 5ª cuerda)")
    void testDetect110Hz() {
        float[] buffer = generateSineWave(110.0, SAMPLE_RATE, BUFFER_SIZE);
        PitchDetectionResult result = detector.detect(buffer, SAMPLE_RATE);

        assertThat(result.isPitched()).isTrue();
        assertThat(result.frequencyHz()).isCloseTo(110.0, offset(1.5));
        assertThat(result.note()).isEqualTo(Note.A);
        assertThat(result.probability()).isGreaterThan(0.85);
    }

    @Test
    @DisplayName("Debe detectar tono puro de 329.63 Hz (E4 - 1ª cuerda)")
    void testDetectE4() {
        float[] buffer = generateSineWave(329.63, SAMPLE_RATE, BUFFER_SIZE);
        PitchDetectionResult result = detector.detect(buffer, SAMPLE_RATE);

        assertThat(result.isPitched()).isTrue();
        assertThat(result.frequencyHz()).isCloseTo(329.63, offset(2.0));
        assertThat(result.note()).isEqualTo(Note.E);
    }

    @Test
    @DisplayName("Debe detectar silencio o ruido blanco como no afinado")
    void testDetectSilence() {
        float[] silenceBuffer = new float[BUFFER_SIZE];
        PitchDetectionResult result = detector.detect(silenceBuffer, SAMPLE_RATE);

        assertThat(result.isPitched()).isFalse();
        assertThat(result.note()).isNull();
    }

    private float[] generateSineWave(double freqHz, float sampleRate, int samples) {
        float[] buffer = new float[samples];
        for (int i = 0; i < samples; i++) {
            buffer[i] = (float) Math.sin(2.0 * Math.PI * freqHz * i / sampleRate);
        }
        return buffer;
    }
}

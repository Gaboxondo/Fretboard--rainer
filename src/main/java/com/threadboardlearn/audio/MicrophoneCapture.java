package com.threadboardlearn.audio;

import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.dsp.PitchDetector;
import com.threadboardlearn.dsp.YinPitchDetector;

import lombok.Setter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captura audio del micrófono del sistema en tiempo real y ejecuta la detección de tono.
 */
public class MicrophoneCapture {
    public interface Listener {
        void onPitchDetected(PitchDetectionResult result);
    }

    private static final float SAMPLE_RATE = 44100.0f;
    private static final int HOP_SIZE = 2048;    // ~46 ms por lectura (mantiene baja latencia)
    private static final int WINDOW_SIZE = 4096; // ~93 ms ventana de análisis YIN (alta resolución en 5ª y 6ª cuerda)

    private final PitchDetector pitchDetector = new YinPitchDetector();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private TargetDataLine dataLine;
    private Thread captureThread;

    @Setter
    private Listener listener;

    @Setter
    private Mixer.Info selectedMixer;

    public synchronized void start() throws LineUnavailableException {
        if (running.get()) return;

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (selectedMixer != null) {
            Mixer mixer = AudioSystem.getMixer(selectedMixer);
            dataLine = (TargetDataLine) mixer.getLine(info);
        } else {
            dataLine = (TargetDataLine) AudioSystem.getLine(info);
        }

        dataLine.open(format, HOP_SIZE * 4);
        dataLine.start();
        running.set(true);

        captureThread = Thread.ofVirtual().name("Microphone-Capture-Thread").start(this::captureLoop);
    }

    public synchronized void stop() {
        running.set(false);
        if (dataLine != null) {
            try {
                dataLine.stop();
                dataLine.close();
            } catch (Exception ignored) {
            }
        }
        if (captureThread != null) {
            captureThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public static List<Mixer.Info> getAvailableInputMixers() {
        List<Mixer.Info> inputs = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mInfo);
                if (mixer.isLineSupported(info)) {
                    inputs.add(mInfo);
                }
            } catch (Exception ignored) {
            }
        }
        return inputs;
    }

    private void captureLoop() {
        byte[] byteBuffer = new byte[HOP_SIZE * 2]; // 16-bit = 2 bytes por muestra
        float[] hopBuffer = new float[HOP_SIZE];
        float[] analysisWindow = new float[WINDOW_SIZE];

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            int bytesRead = dataLine.read(byteBuffer, 0, byteBuffer.length);
            if (bytesRead <= 0) continue;

            int samplesRead = bytesRead / 2;
            for (int i = 0; i < samplesRead; i++) {
                // Conversión de 16-bit signed PCM little-endian a float (-1.0 a 1.0)
                int low = byteBuffer[2 * i] & 0xFF;
                int high = byteBuffer[2 * i + 1];
                short sample = (short) ((high << 8) | low);
                hopBuffer[i] = sample / 32768.0f;
            }

            for (int i = samplesRead; i < HOP_SIZE; i++) {
                hopBuffer[i] = 0.0f;
            }

            // Desplazar muestras anteriores hacia la izquierda (ventana deslizante)
            System.arraycopy(analysisWindow, HOP_SIZE, analysisWindow, 0, HOP_SIZE);
            // Insertar nuevas muestras a la derecha
            System.arraycopy(hopBuffer, 0, analysisWindow, HOP_SIZE, HOP_SIZE);

            PitchDetectionResult result = pitchDetector.detect(analysisWindow, SAMPLE_RATE);
            if (listener != null) {
                listener.onPitchDetected(result);
            }
        }
    }
}

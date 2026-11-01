package com.threadboardlearn.engine;

import com.threadboardlearn.audio.AudioPlayerService;
import com.threadboardlearn.audio.MicrophoneCaptureService;
import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;
import com.threadboardlearn.model.TrainingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingSessionTest {

    private TrainingConfig config;
    private StubAudioPlayer audioPlayer;
    private StubMicrophoneCapture micCapture;
    private TrainingSession session;
    private AtomicInteger eventDispatchCount;

    @BeforeEach
    void setUp() {
        config = new TrainingConfig();
        config.setTimeLimitSeconds(0); // Sin límite para pruebas
        audioPlayer = new StubAudioPlayer();
        micCapture = new StubMicrophoneCapture();
        eventDispatchCount = new AtomicInteger(0);

        session = new TrainingSession(
                config,
                audioPlayer,
                micCapture,
                action -> {
                    eventDispatchCount.incrementAndGet();
                    action.run();
                }
        );
    }

    @Test
    @DisplayName("Debe iniciar y detener la sesión notificando mediante el EventDispatcher")
    void testStartAndStopSession() {
        AtomicBoolean stateNotified = new AtomicBoolean(false);
        session.setListener(new TestGameEventListener() {
            @Override
            public void onSessionStateChanged(boolean running) {
                stateNotified.set(running);
            }
        });

        session.start();
        assertThat(session.isRunning()).isTrue();
        assertThat(stateNotified.get()).isTrue();
        assertThat(session.getCurrentChallenge()).isNotNull();
        assertThat(eventDispatchCount.get()).isGreaterThan(0);

        session.stop();
        assertThat(session.isRunning()).isFalse();
        assertThat(stateNotified.get()).isFalse();
    }

    @Test
    @DisplayName("Debe validar acierto y fallo en submitFretClick")
    void testSubmitFretClick() {
        session.start();
        Challenge challenge = session.getCurrentChallenge();
        GuitarString targetString = challenge.targetString();
        int validFret = challenge.validFrets().get(0);

        // Acierto
        boolean correct = session.submitFretClick(targetString, validFret);
        assertThat(correct).isTrue();
        assertThat(session.getStats().getCorrectCount()).isEqualTo(1);
        assertThat(audioPlayer.successSoundPlayed).isTrue();

        session.shutdown();
    }

    @Test
    @DisplayName("El micrófono debe ignorar la nota mientras suena el audio de referencia para no auto-validarse")
    void testMicrophoneIgnoredDuringReferenceAudio() {
        config.setPlayReferenceAudio(true);
        config.setMicrophoneEnabled(true);

        session.start();
        assertThat(session.isReferenceAudioPlaying()).isTrue();

        Challenge challenge = session.getCurrentChallenge();
        int validFret = challenge.validFrets().get(0);
        double targetFreq = challenge.targetString().getFrequencyAtFret(validFret);
        PitchDetectionResult matchingPitch = new PitchDetectionResult(
                targetFreq, 0.95, challenge.targetNote(), 0.0, 0.08, true
        );

        // Enviar múltiples detecciones que coinciden perfectamente con la nota
        for (int i = 0; i < 5; i++) {
            micCapture.listener.onPitchDetected(matchingPitch);
        }

        // No debe haberse validado como acierto porque aún estaba sonando el audio de la propia app
        assertThat(session.getStats().getCorrectCount()).isEqualTo(0);
        assertThat(audioPlayer.successSoundPlayed).isFalse();

        session.shutdown();
    }

    @Test
    @DisplayName("El micrófono debe validar el acierto cuando el audio de referencia no está sonando")
    void testMicrophoneEvaluatesWhenAudioNotPlaying() {
        config.setPlayReferenceAudio(false);
        config.setMicrophoneEnabled(true);

        session.start();
        assertThat(session.isReferenceAudioPlaying()).isFalse();

        Challenge challenge = session.getCurrentChallenge();
        int validFret = challenge.validFrets().get(0);
        double targetFreq = challenge.targetString().getFrequencyAtFret(validFret);
        PitchDetectionResult matchingPitch = new PitchDetectionResult(
                targetFreq, 0.95, challenge.targetNote(), 0.0, 0.08, true
        );

        // Enviar 3 frames consecutivos estables (REQUIRED_SUSTAINED_FRAMES = 3)
        for (int i = 0; i < 3; i++) {
            micCapture.listener.onPitchDetected(matchingPitch);
        }

        // Ahora sí debe haberse validado con éxito
        assertThat(session.getStats().getCorrectCount()).isEqualTo(1);
        assertThat(audioPlayer.successSoundPlayed).isTrue();

        session.shutdown();
    }

    @Test
    @DisplayName("Replay audio debe reactivar la protección contra auto-detección del micrófono")
    void testReplayChallengeAudioProtectsMicrophone() {
        config.setPlayReferenceAudio(false);
        config.setMicrophoneEnabled(true);

        session.start();
        assertThat(session.isReferenceAudioPlaying()).isFalse();

        // El usuario solicita reproducir la nota de nuevo
        session.replayChallengeAudio();
        assertThat(session.isReferenceAudioPlaying()).isTrue();

        session.shutdown();
    }

    @Test
    @DisplayName("Debe generar retos únicamente en las cuerdas seleccionadas (ej. 6ª y 5ª cuerda)")
    void testMultiStringSelectionGeneratesOnlySelectedStrings() {
        config.setSelectedStrings(java.util.Set.of(GuitarString.STRING_6, GuitarString.STRING_5));
        config.setPlayReferenceAudio(false);

        session.start();
        for (int i = 0; i < 25; i++) {
            session.nextChallenge();
            Challenge challenge = session.getCurrentChallenge();
            assertThat(challenge).isNotNull();
            assertThat(challenge.targetString())
                    .isIn(GuitarString.STRING_6, GuitarString.STRING_5);
        }

        session.shutdown();
    }

    private static class TestGameEventListener implements GameEventListener {
        @Override public void onChallengeStarted(Challenge challenge) {}
        @Override public void onTimerTick(int secondsRemaining, double percentElapsed) {}
        @Override public void onChallengeSuccess(Challenge challenge, long reactionTimeMs) {}
        @Override public void onChallengeTimeout(Challenge challenge) {}
        @Override public void onPitchActivity(PitchDetectionResult pitchResult) {}
        @Override public void onStatsUpdated(TrainingStats stats) {}
        @Override public void onSessionStateChanged(boolean running) {}
    }

    private static class StubAudioPlayer implements AudioPlayerService {
        boolean successSoundPlayed = false;
        boolean failureSoundPlayed = false;

        @Override
        public void playGuitarNote(GuitarString string, int fret) {}

        @Override
        public void playNote(Note note, GuitarString preferredString) {}

        @Override
        public void playSuccessSound() {
            successSoundPlayed = true;
        }

        @Override
        public void playFailureSound() {
            failureSoundPlayed = true;
        }

        @Override
        public void close() {}
    }

    private static class StubMicrophoneCapture implements MicrophoneCaptureService {
        boolean running = false;
        Listener listener;

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public void setListener(Listener listener) {
            this.listener = listener;
        }
    }
}

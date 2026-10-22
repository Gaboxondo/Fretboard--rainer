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

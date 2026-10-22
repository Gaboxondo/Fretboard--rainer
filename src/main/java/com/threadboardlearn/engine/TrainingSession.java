package com.threadboardlearn.engine;

import com.threadboardlearn.audio.AudioPlayerService;
import com.threadboardlearn.audio.MicrophoneCaptureService;
import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;
import com.threadboardlearn.model.TrainingConfig;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Motor central de la sesión de entrenamiento.
 * Coordina los retos aleatorios, el temporizador, el audio y la detección por micrófono.
 * Totalmente desacoplado de la plataforma gráfica (Desktop / Android).
 */
public class TrainingSession {
    @Getter
    private final TrainingConfig config;
    private final AudioPlayerService audioPlayer;
    private final MicrophoneCaptureService micCapture;
    private final EventDispatcher eventDispatcher;

    @Getter
    private final TrainingStats stats = new TrainingStats();
    @Getter
    private final AdaptiveLearningEngine adaptiveEngine = new AdaptiveLearningEngine();
    private final Random random = new Random();

    @Setter
    private GameEventListener listener;
    @Getter
    private Challenge currentChallenge;
    private Note lastNote = null;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean challengeCompleted = new AtomicBoolean(false);

    private final ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timerTask;

    private int sustainedTargetNoteFrames = 0;
    private static final int REQUIRED_SUSTAINED_FRAMES = 3; // ~140 ms de tono estable
    private double lastCandidateFrequency = 0.0;

    public TrainingSession(TrainingConfig config, AudioPlayerService audioPlayer, MicrophoneCaptureService micCapture) {
        this(config, audioPlayer, micCapture, Runnable::run);
    }

    public TrainingSession(TrainingConfig config, AudioPlayerService audioPlayer, MicrophoneCaptureService micCapture, EventDispatcher eventDispatcher) {
        this.config = config;
        this.audioPlayer = audioPlayer;
        this.micCapture = micCapture;
        this.eventDispatcher = eventDispatcher != null ? eventDispatcher : Runnable::run;

        // Conectar callback del micrófono si está presente
        if (this.micCapture != null) {
            this.micCapture.setListener(this::handleMicrophonePitch);
        }
    }

    public TrainingConfig getConfig() { return config; }
    public TrainingStats getStats() { return stats; }
    public AdaptiveLearningEngine getAdaptiveEngine() { return adaptiveEngine; }
    public Challenge getCurrentChallenge() { return currentChallenge; }
    public void setListener(GameEventListener listener) { this.listener = listener; }
    public GameEventListener getListener() { return listener; }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);

        if (config.isMicrophoneEnabled() && micCapture != null) {
            try {
                micCapture.start();
            } catch (Exception e) {
                System.err.println("No se pudo iniciar el micrófono: " + e.getMessage());
            }
        }

        if (listener != null) {
            eventDispatcher.dispatch(() -> {
                listener.onSessionStateChanged(true);
                listener.onStatsUpdated(stats);
            });
        }

        nextChallenge();
    }

    public synchronized void stop() {
        running.set(false);
        stopTimer();
        if (micCapture != null) {
            micCapture.stop();
        }

        if (listener != null) {
            eventDispatcher.dispatch(() -> listener.onSessionStateChanged(false));
        }
    }

    /**
     * Genera el siguiente reto aleatorio.
     */
    public synchronized void nextChallenge() {
        if (!running.get()) return;

        stopTimer();
        challengeCompleted.set(false);
        sustainedTargetNoteFrames = 0;

        // 1. Seleccionar Cuerda
        GuitarString targetString;
        if (config.isAllStrings()) {
            GuitarString[] strings = GuitarString.values();
            targetString = strings[random.nextInt(strings.length)];
        } else {
            targetString = config.getSelectedString();
        }

        // 2. Seleccionar Nota (con probabilidad ponderada adaptativa o aleatorio uniforme)
        List<Note> availableNotes = getAvailableNotes();
        Note targetNote;
        if (config.isAdaptiveLearning()) {
            targetNote = adaptiveEngine.selectNextNote(availableNotes, lastNote, random);
        } else {
            do {
                targetNote = availableNotes.get(random.nextInt(availableNotes.size()));
            } while (availableNotes.size() > 1 && targetNote == lastNote);
        }
        lastNote = targetNote;

        // 3. Obtener trastes válidos en la cuerda y metadata del reto adaptativo
        List<Integer> validFrets = targetString.findFretsForNote(targetNote, config.getMaxFret());
        boolean isFocus = config.isAdaptiveLearning() && adaptiveEngine.isFocusNote(targetNote, availableNotes);
        double probability = config.isAdaptiveLearning() ? adaptiveEngine.getProbability(targetNote, availableNotes) : (1.0 / availableNotes.size());

        currentChallenge = new Challenge(targetNote, targetString, validFrets, System.currentTimeMillis(), isFocus, probability);

        // 4. Notificar a la UI
        Challenge ch = currentChallenge;
        if (listener != null) {
            eventDispatcher.dispatch(() -> listener.onChallengeStarted(ch));
        }

        // 5. Reproducir audio de referencia si está activado
        if (config.isPlayReferenceAudio() && !validFrets.isEmpty() && audioPlayer != null) {
            audioPlayer.playGuitarNote(targetString, validFrets.get(0));
        }

        // 6. Iniciar cronómetro si hay límite de tiempo
        startTimer();
    }

    /**
     * Vuelve a reproducir el sonido de la nota del reto actual.
     */
    public void replayChallengeAudio() {
        if (currentChallenge != null && !currentChallenge.validFrets().isEmpty() && audioPlayer != null) {
            audioPlayer.playGuitarNote(currentChallenge.targetString(), currentChallenge.validFrets().get(0));
        }
    }

    /**
     * Manejo del clic en el diapasón visual desde la UI.
     */
    public synchronized boolean submitFretClick(GuitarString string, int fret) {
        if (!running.get() || challengeCompleted.get() || currentChallenge == null) {
            return false;
        }

        if (currentChallenge.isValidFret(string, fret)) {
            handleSuccess();
            return true;
        } else {
            // Nota incorrecta: registrar fallo en el motor adaptativo y estadísticas
            if (config.isAdaptiveLearning()) {
                adaptiveEngine.recordMistake(currentChallenge.targetNote(), currentChallenge.targetString());
            }
            stats.recordMistake();

            if (listener != null) {
                eventDispatcher.dispatch(() -> listener.onStatsUpdated(stats));
            }

            // Reproducir la nota que el usuario ha tocado para su oído
            if (audioPlayer != null) {
                audioPlayer.playGuitarNote(string, fret);
            }
            return false;
        }
    }

    /**
     * Manejo de la señal del micrófono con filtrado robusto contra voces y ruido de fondo.
     */
    private void handleMicrophonePitch(PitchDetectionResult result) {
        if (!running.get()) return;

        // Notificar nivel / tono a la UI para el VU meter y afinador
        if (listener != null) {
            eventDispatcher.dispatch(() -> listener.onPitchActivity(result));
        }

        if (challengeCompleted.get() || currentChallenge == null) {
            return;
        }

        // 1. Puerta de ruido (Noise Gate): Si la energía no supera el umbral configurado, descartar
        if (result.rms() < config.getMicSensitivity() || !result.isPitched()) {
            sustainedTargetNoteFrames = 0;
            lastCandidateFrequency = 0.0;
            return;
        }

        // 2. Confianza mínima de la detección
        if (result.probability() < config.getMinConfidence()) {
            sustainedTargetNoteFrames = 0;
            lastCandidateFrequency = 0.0;
            return;
        }

        // 3. Verificación estricta de la nota musical
        if (result.note() != currentChallenge.targetNote()) {
            sustainedTargetNoteFrames = 0;
            lastCandidateFrequency = 0.0;
            return;
        }

        // 4. Verificación de octava y armónico de la cuerda (tolerancia fina de ±35 cents)
        if (config.isMatchExactOctave()) {
            if (!currentChallenge.matchesStringFrequency(result.frequencyHz(), 35.0)) {
                sustainedTargetNoteFrames = 0;
                lastCandidateFrequency = 0.0;
                return;
            }
        }

        // 4. Estabilidad física de la cuerda: las cuerdas pulsadas mantienen una frecuencia constante (< 18 cents)
        // mientras que las voces humanas modulan continuamente (vibrato/prosodia de habla)
        if (lastCandidateFrequency > 0.0) {
            double centsDrift = Math.abs(1200.0 * (Math.log(result.frequencyHz() / lastCandidateFrequency) / Math.log(2.0)));
            if (centsDrift > 20.0) {
                // Tono inestable (habla humana o ruido transitorio) -> reiniciar
                sustainedTargetNoteFrames = 1;
                lastCandidateFrequency = result.frequencyHz();
                return;
            }
        }

        lastCandidateFrequency = result.frequencyHz();
        sustainedTargetNoteFrames++;

        if (sustainedTargetNoteFrames >= REQUIRED_SUSTAINED_FRAMES) {
            handleSuccess();
        }
    }

    private synchronized void handleSuccess() {
        if (challengeCompleted.getAndSet(true)) return;
        stopTimer();

        long reactionTime = System.currentTimeMillis() - currentChallenge.startTimeMs();
        stats.recordSuccess(reactionTime);
        if (config.isAdaptiveLearning()) {
            adaptiveEngine.recordSuccess(currentChallenge.targetNote(), currentChallenge.targetString(), reactionTime);
        }
        if (audioPlayer != null) {
            audioPlayer.playSuccessSound();
        }

        Challenge ch = currentChallenge;
        if (listener != null) {
            eventDispatcher.dispatch(() -> {
                listener.onChallengeSuccess(ch, reactionTime);
                listener.onStatsUpdated(stats);
            });
        }

        // Breve pausa antes del siguiente reto para ver el acierto
        timerScheduler.schedule(this::nextChallenge, 800, TimeUnit.MILLISECONDS);
    }

    private synchronized void handleTimeout() {
        if (challengeCompleted.getAndSet(true)) return;
        stopTimer();

        stats.recordTimeout();
        if (config.isAdaptiveLearning()) {
            adaptiveEngine.recordTimeout(currentChallenge.targetNote(), currentChallenge.targetString());
        }
        if (audioPlayer != null) {
            audioPlayer.playFailureSound();
        }

        Challenge ch = currentChallenge;
        if (listener != null) {
            eventDispatcher.dispatch(() -> {
                listener.onChallengeTimeout(ch);
                listener.onStatsUpdated(stats);
            });
        }

        // Pausa breve para que el usuario vea la solución antes del siguiente reto
        timerScheduler.schedule(this::nextChallenge, 1400, TimeUnit.MILLISECONDS);
    }

    public synchronized void resetStats() {
        stats.reset();
        adaptiveEngine.reset();
        if (listener != null) {
            eventDispatcher.dispatch(() -> listener.onStatsUpdated(stats));
        }
    }

    public List<Note> getAvailableNotes() {
        return config.isIncludeSharps() ? Note.getAllNotes() : Note.getNaturalNotes();
    }

    private void startTimer() {
        int limitSeconds = config.getTimeLimitSeconds();
        if (limitSeconds <= 0) {
            // Sin límite de tiempo
            if (listener != null) {
                eventDispatcher.dispatch(() -> listener.onTimerTick(0, 0.0));
            }
            return;
        }

        long startTime = System.currentTimeMillis();
        long totalDurationMs = limitSeconds * 1000L;

        timerTask = timerScheduler.scheduleAtFixedRate(() -> {
            if (!running.get() || challengeCompleted.get()) return;

            long elapsed = System.currentTimeMillis() - startTime;
            long remainingMs = Math.max(0, totalDurationMs - elapsed);
            int secondsRemaining = (int) Math.ceil(remainingMs / 1000.0);
            double percentElapsed = Math.min(1.0, (double) elapsed / totalDurationMs);

            if (listener != null) {
                eventDispatcher.dispatch(() -> listener.onTimerTick(secondsRemaining, percentElapsed));
            }

            if (remainingMs <= 0) {
                handleTimeout();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel(true);
            timerTask = null;
        }
    }

    public void shutdown() {
        stop();
        timerScheduler.shutdown();
        if (audioPlayer != null) {
            audioPlayer.close();
        }
    }
}

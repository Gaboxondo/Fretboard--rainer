package com.threadboardlearn.engine;

import com.threadboardlearn.dsp.PitchDetectionResult;

/**
 * Escuchador de eventos del ciclo de entrenamiento para actualizar la interfaz gráfica.
 */
public interface GameEventListener {
    void onChallengeStarted(Challenge challenge);
    void onTimerTick(int secondsRemaining, double percentElapsed);
    void onChallengeSuccess(Challenge challenge, long reactionTimeMs);
    void onChallengeTimeout(Challenge challenge);
    void onPitchActivity(PitchDetectionResult pitchResult);
    void onStatsUpdated(TrainingStats stats);
    void onSessionStateChanged(boolean running);
}

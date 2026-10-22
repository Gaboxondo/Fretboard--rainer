package com.threadboardlearn.audio;

import com.threadboardlearn.dsp.PitchDetectionResult;

/**
 * Servicio abstracto de captura de micrófono y detección de tono.
 * Permite implementaciones en Desktop (javax.sound TargetDataLine) y en Android (AudioRecord).
 */
public interface MicrophoneCaptureService {

    @FunctionalInterface
    interface Listener {
        void onPitchDetected(PitchDetectionResult result);
    }

    void start() throws Exception;
    void stop();
    boolean isRunning();
    void setListener(Listener listener);
}

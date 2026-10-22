package com.threadboardlearn.android.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.threadboardlearn.audio.MicrophoneCaptureService
import com.threadboardlearn.dsp.PitchDetector
import com.threadboardlearn.dsp.YinPitchDetector
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementación de MicrophoneCaptureService para Android usando android.media.AudioRecord.
 * Reutiliza el detector matemático YinPitchDetector del núcleo compartido sin modificarlo.
 */
class AndroidAudioRecordCapture : MicrophoneCaptureService {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val HOP_SIZE = 2048    // ~46 ms
        private const val WINDOW_SIZE = 4096 // ~93 ms ventana YIN
    }

    private val pitchDetector: PitchDetector = YinPitchDetector()
    private val running = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var listener: MicrophoneCaptureService.Listener? = null

    override fun setListener(listener: MicrophoneCaptureService.Listener?) {
        this.listener = listener
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    override fun start() {
        if (running.get()) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, HOP_SIZE * 4)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("No se pudo inicializar AudioRecord en Android")
        }

        audioRecord = record
        record.startRecording()
        running.set(true)

        captureThread = Thread({ captureLoop() }, "Android-AudioRecord-Thread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    @Synchronized
    override fun stop() {
        running.set(false)
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {
        } finally {
            audioRecord = null
        }

        captureThread?.interrupt()
        captureThread = null
    }

    override fun isRunning(): Boolean = running.get()

    private fun captureLoop() {
        val shortBuffer = ShortArray(HOP_SIZE)
        val hopBuffer = FloatArray(HOP_SIZE)
        val analysisWindow = FloatArray(WINDOW_SIZE)

        while (running.get() && !Thread.currentThread().isInterrupted) {
            val record = audioRecord ?: break
            val samplesRead = record.read(shortBuffer, 0, HOP_SIZE)
            if (samplesRead <= 0) continue

            for (i in 0 until samplesRead) {
                hopBuffer[i] = shortBuffer[i] / 32768.0f
            }
            for (i in samplesRead until HOP_SIZE) {
                hopBuffer[i] = 0.0f
            }

            // Ventana deslizante YIN: desplazar mitad izquierda y copiar nuevo hop a la derecha
            System.arraycopy(analysisWindow, HOP_SIZE, analysisWindow, 0, HOP_SIZE)
            System.arraycopy(hopBuffer, 0, analysisWindow, HOP_SIZE, HOP_SIZE)

            val result = pitchDetector.detect(analysisWindow, SAMPLE_RATE.toFloat())
            listener?.onPitchDetected(result)
        }
    }
}

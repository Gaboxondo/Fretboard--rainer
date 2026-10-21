package com.threadboardlearn.ui;

import com.threadboardlearn.audio.AudioPlayer;
import com.threadboardlearn.audio.MicrophoneCapture;
import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.engine.Challenge;
import com.threadboardlearn.engine.GameEventListener;
import com.threadboardlearn.engine.TrainingSession;
import com.threadboardlearn.engine.TrainingStats;
import com.threadboardlearn.model.TrainingConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal de la aplicación de escritorio Fretboard Trainer.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements GameEventListener {
    private final TrainingConfig config;
    private final AudioPlayer audioPlayer;
    private final MicrophoneCapture micCapture;
    private final TrainingSession session;

    private final ChallengeDisplayPanel challengePanel;
    private final FretboardView fretboardView;
    private final ControlPanel controlPanel;
    private final StatusBar statusBar;

    public MainFrame() {
        super("Guitar Fretboard Trainer — Aprende las Notas de la Guitarra");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 720));
        setPreferredSize(new Dimension(1100, 780));

        // 1. Inicialización de la lógica del sistema
        this.config = new TrainingConfig();
        this.audioPlayer = new AudioPlayer();
        this.micCapture = new MicrophoneCapture();
        this.session = new TrainingSession(config, audioPlayer, micCapture);
        this.session.setListener(this);

        // 2. Inicialización de componentes gráficos
        this.challengePanel = new ChallengeDisplayPanel();
        this.fretboardView = new FretboardView();
        this.controlPanel = new ControlPanel(config);
        this.statusBar = new StatusBar();

        // 3. Conectar callbacks
        setupCallbacks();

        // 4. Montar layout principal
        buildLayout();

        // 5. Cierre seguro de hilos y audio
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                session.shutdown();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void setupCallbacks() {
        challengePanel.setOnReplayAudio(session::replayChallengeAudio);

        fretboardView.setOnFretClicked((string, fret) -> {
            session.submitFretClick(string, fret);
        });

        controlPanel.setCallbacks(
                () -> {
                    if (session.isRunning()) {
                        session.stop();
                        challengePanel.reset();
                    } else {
                        session.start();
                    }
                },
                session::nextChallenge,
                fretboardView::setMaxFret
        );
    }

    private void buildLayout() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 10));
        contentPane.setBackground(new Color(20, 22, 25));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));

        // Parte superior: Desafío actual
        contentPane.add(challengePanel, BorderLayout.NORTH);

        // Centro: Diapasón de guitarra con contenedor estilizado
        JPanel fretboardContainer = new JPanel(new BorderLayout());
        fretboardContainer.setOpaque(false);
        fretboardContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 75), 1, true),
                "Diapasón Interactivo (Haz clic en el traste y cuerda correspondiente o toca en tu guitarra)"
        ));
        fretboardContainer.add(fretboardView, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(fretboardContainer, BorderLayout.CENTER);
        centerPanel.add(controlPanel, BorderLayout.SOUTH);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // Parte inferior: Barra de estado y VU meter
        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.add(contentPane, BorderLayout.CENTER);
        mainWrapper.add(statusBar, BorderLayout.SOUTH);

        setContentPane(mainWrapper);
    }

    // --- Implementación de GameEventListener ---

    @Override
    public void onChallengeStarted(Challenge challenge) {
        challengePanel.showChallenge(challenge);
        fretboardView.setChallenge(challenge);
    }

    @Override
    public void onTimerTick(int secondsRemaining, double percentElapsed) {
        challengePanel.updateTimer(secondsRemaining, percentElapsed);
    }

    @Override
    public void onChallengeSuccess(Challenge challenge, long reactionTimeMs) {
        challengePanel.showSuccess(reactionTimeMs);
        fretboardView.showSuccessFeedback(challenge);
    }

    @Override
    public void onChallengeTimeout(Challenge challenge) {
        challengePanel.showTimeout(challenge);
        fretboardView.showTimeoutSolution(challenge);
    }

    @Override
    public void onPitchActivity(PitchDetectionResult pitchResult) {
        statusBar.updatePitchResult(pitchResult);
    }

    @Override
    public void onStatsUpdated(TrainingStats stats) {
        statusBar.updateStats(stats);
    }

    @Override
    public void onSessionStateChanged(boolean running) {
        controlPanel.updateSessionState(running);
    }
}

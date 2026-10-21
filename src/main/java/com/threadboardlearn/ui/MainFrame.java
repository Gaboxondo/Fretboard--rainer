package com.threadboardlearn.ui;

import com.threadboardlearn.audio.AudioPlayer;
import com.threadboardlearn.audio.MicrophoneCapture;
import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.engine.Challenge;
import com.threadboardlearn.engine.GameEventListener;
import com.threadboardlearn.engine.TrainingSession;
import com.threadboardlearn.engine.TrainingStats;
import com.threadboardlearn.i18n.I18n;
import com.threadboardlearn.i18n.Language;
import com.threadboardlearn.model.TrainingConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
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

    private final JLabel appSubtitleLabel = new JLabel();
    private final JLabel langTextLabel = new JLabel();
    private final JComboBox<Language> languageComboBox = new JComboBox<>(Language.values());
    private final JPanel fretboardContainer = new JPanel(new BorderLayout());
    private final TitledBorder fretboardBorder;

    public MainFrame() {
        super(I18n.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 720));
        setPreferredSize(new Dimension(1100, 780));

        // Cargar icono de la aplicación
        try {
            java.net.URL iconUrl = getClass().getResource("/icons/app-icon.png");
            if (iconUrl != null) {
                setIconImage(new ImageIcon(iconUrl).getImage());
            }
        } catch (Exception ignored) {
        }

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

        this.fretboardBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 75), 1, true),
                I18n.get("fretboard.title")
        );

        // 3. Conectar callbacks
        setupCallbacks();

        // 4. Montar layout principal
        buildLayout();

        // 5. Escuchar cambios de idioma
        I18n.addListener(lang -> SwingUtilities.invokeLater(this::updateI18n));
        updateI18n();

        // 6. Cierre seguro de hilos y audio
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

        languageComboBox.addActionListener(e -> {
            Language selected = (Language) languageComboBox.getSelectedItem();
            if (selected != null && selected != I18n.getLanguage()) {
                I18n.setLanguage(selected);
            }
        });
    }

    private void buildLayout() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 10));
        contentPane.setBackground(new Color(20, 22, 25));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 16, 0, 16));

        // Barra superior: Marca / Título y Selector de Idioma
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 6, 4));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);
        JLabel iconLabel = new JLabel("🎸");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        JLabel titleLabel = new JLabel("Fretboard Trainer");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(new Color(230, 235, 245));

        appSubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        appSubtitleLabel.setForeground(new Color(140, 150, 165));

        brandPanel.add(iconLabel);
        brandPanel.add(titleLabel);
        brandPanel.add(appSubtitleLabel);

        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        langPanel.setOpaque(false);
        JLabel langIcon = new JLabel("🌐");
        langIcon.setFont(new Font("SansSerif", Font.PLAIN, 14));

        langTextLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        langTextLabel.setForeground(new Color(180, 185, 195));

        languageComboBox.setPreferredSize(new Dimension(115, 26));
        languageComboBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        languageComboBox.setFocusable(false);

        langPanel.add(langIcon);
        langPanel.add(langTextLabel);
        langPanel.add(languageComboBox);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(langPanel, BorderLayout.EAST);

        // Contenedor superior (TopBar + ChallengePanel)
        JPanel topContainer = new JPanel(new BorderLayout(0, 4));
        topContainer.setOpaque(false);
        topContainer.add(topBar, BorderLayout.NORTH);
        topContainer.add(challengePanel, BorderLayout.CENTER);

        contentPane.add(topContainer, BorderLayout.NORTH);

        // Centro: Diapasón de guitarra con contenedor estilizado
        fretboardContainer.setOpaque(false);
        fretboardContainer.setBorder(fretboardBorder);
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

    private void updateI18n() {
        setTitle(I18n.get("app.title"));
        appSubtitleLabel.setText("— " + I18n.get("app.subtitle"));
        langTextLabel.setText(I18n.get("app.language"));
        languageComboBox.setSelectedItem(I18n.getLanguage());

        fretboardBorder.setTitle(I18n.get("fretboard.title"));
        fretboardContainer.repaint();

        challengePanel.updateI18n();
        fretboardView.updateI18n();
        controlPanel.updateI18n();
        statusBar.updateI18n();
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
        com.threadboardlearn.model.Note focus = session.getAdaptiveEngine().getMostChallengingNote(session.getAvailableNotes());
        double prob = focus != null ? session.getAdaptiveEngine().getProbability(focus, session.getAvailableNotes()) : 0.0;
        statusBar.updateStats(stats, focus, prob);
    }

    @Override
    public void onSessionStateChanged(boolean running) {
        controlPanel.updateSessionState(running);
    }
}

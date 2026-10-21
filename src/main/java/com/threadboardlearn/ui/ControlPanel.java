package com.threadboardlearn.ui;

import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.TrainingConfig;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Panel de controles de configuración de niveles, cuerdas, sostenidos y micrófono.
 */
@SuppressWarnings("serial")
public class ControlPanel extends JPanel {
    private final JComboBox<String> stringComboBox = new JComboBox<>();
    private final JCheckBox sharpsCheckBox = new JCheckBox("Incluir sostenidos (#)", false);
    private final JComboBox<String> timeLimitComboBox = new JComboBox<>(new String[]{
            "3 segundos", "5 segundos", "8 segundos", "10 segundos", "15 segundos", "Sin límite (Práctica)"
    });
    private final JCheckBox playAudioCheckBox = new JCheckBox("Reproducir nota de referencia", true);
    private final JCheckBox micEnabledCheckBox = new JCheckBox("Escuchar por micrófono", true);
    private final JComboBox<String> maxFretComboBox = new JComboBox<>(new String[]{
            "12 trastes (Estándar)", "15 trastes", "24 trastes"
    });

    private final JCheckBox exactOctaveCheckBox = new JCheckBox("Filtrar octava cuerda (anti-voz)", true);
    private final JSlider noiseGateSlider = new JSlider(10, 100, 45); // 0.010 a 0.100 RMS

    private final JButton startStopButton = new JButton("▶ Iniciar Entrenamiento");
    private final JButton nextButton = new JButton("⏭ Siguiente Reto");

    private final TrainingConfig config;
    private Runnable onStartStop;
    private Runnable onNext;
    private Consumer<Integer> onMaxFretChanged;

    public ControlPanel(TrainingConfig config) {
        this.config = config;
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 75), 1, true),
                "Configuración de Entrenamiento"
        ));

        initStringCombo();
        timeLimitComboBox.setSelectedIndex(1); // 5 segundos por defecto

        // Grid con 4 filas y 2 columnas de opciones
        JPanel optionsGrid = new JPanel(new GridLayout(4, 2, 12, 6));
        optionsGrid.setOpaque(false);
        optionsGrid.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        // Fila 1: Cuerda y Sostenidos
        JPanel stringBox = new JPanel(new BorderLayout(5, 0));
        stringBox.setOpaque(false);
        JLabel strLbl = new JLabel("Cuerda:");
        strLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        stringBox.add(strLbl, BorderLayout.WEST);
        stringBox.add(stringComboBox, BorderLayout.CENTER);
        optionsGrid.add(stringBox);
        optionsGrid.add(sharpsCheckBox);

        // Fila 2: Tiempo y Trastes
        JPanel timeBox = new JPanel(new BorderLayout(5, 0));
        timeBox.setOpaque(false);
        JLabel timeLbl = new JLabel("Tiempo:");
        timeLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        timeBox.add(timeLbl, BorderLayout.WEST);
        timeBox.add(timeLimitComboBox, BorderLayout.CENTER);
        optionsGrid.add(timeBox);

        JPanel fretBox = new JPanel(new BorderLayout(5, 0));
        fretBox.setOpaque(false);
        JLabel fretLbl = new JLabel("Rango:");
        fretLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        fretBox.add(fretLbl, BorderLayout.WEST);
        fretBox.add(maxFretComboBox, BorderLayout.CENTER);
        optionsGrid.add(fretBox);

        // Fila 3: Audio de referencia y Micrófono
        optionsGrid.add(playAudioCheckBox);
        optionsGrid.add(micEnabledCheckBox);

        // Fila 4: Filtro de octava de guitarra y Puerta de ruido
        optionsGrid.add(exactOctaveCheckBox);

        JPanel noiseGatePanel = new JPanel(new BorderLayout(6, 0));
        noiseGatePanel.setOpaque(false);
        JLabel gateLbl = new JLabel("Filtro ruido micro:");
        gateLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        gateLbl.setToolTipText("Aumenta el valor para ignorar conversaciones y ruidos de fondo en la habitación");
        noiseGateSlider.setToolTipText("Puerta de ruido: solo detecta notas con volumen superior a este umbral");
        noiseGatePanel.add(gateLbl, BorderLayout.WEST);
        noiseGatePanel.add(noiseGateSlider, BorderLayout.CENTER);
        optionsGrid.add(noiseGatePanel);

        // Panel de acciones (Iniciar / Siguiente)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        startStopButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        startStopButton.setBackground(new Color(40, 167, 69));
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setFocusPainted(false);
        startStopButton.setPreferredSize(new Dimension(200, 38));

        nextButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        nextButton.setEnabled(false);
        nextButton.setFocusPainted(false);
        nextButton.setPreferredSize(new Dimension(140, 38));

        actionPanel.add(startStopButton);
        actionPanel.add(nextButton);

        add(optionsGrid, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.EAST);

        setupListeners();
    }

    private void initStringCombo() {
        stringComboBox.addItem("Todas las cuerdas (Aleatorio)");
        for (GuitarString s : GuitarString.values()) {
            stringComboBox.addItem(s.getLabel());
        }
    }

    private void setupListeners() {
        stringComboBox.addActionListener(e -> {
            int idx = stringComboBox.getSelectedIndex();
            if (idx == 0) {
                config.setSelectedString(null);
            } else {
                config.setSelectedString(GuitarString.fromNumber(idx));
            }
        });

        sharpsCheckBox.addActionListener(e -> config.setIncludeSharps(sharpsCheckBox.isSelected()));

        timeLimitComboBox.addActionListener(e -> {
            int idx = timeLimitComboBox.getSelectedIndex();
            int seconds = switch (idx) {
                case 0 -> 3;
                case 1 -> 5;
                case 2 -> 8;
                case 3 -> 10;
                case 4 -> 15;
                default -> 0; // Sin límite
            };
            config.setTimeLimitSeconds(seconds);
        });

        maxFretComboBox.addActionListener(e -> {
            int idx = maxFretComboBox.getSelectedIndex();
            int frets = switch (idx) {
                case 1 -> 15;
                case 2 -> 24;
                default -> 12;
            };
            config.setMaxFret(frets);
            if (onMaxFretChanged != null) {
                onMaxFretChanged.accept(frets);
            }
        });

        playAudioCheckBox.addActionListener(e -> config.setPlayReferenceAudio(playAudioCheckBox.isSelected()));
        micEnabledCheckBox.addActionListener(e -> config.setMicrophoneEnabled(micEnabledCheckBox.isSelected()));
        exactOctaveCheckBox.addActionListener(e -> config.setMatchExactOctave(exactOctaveCheckBox.isSelected()));

        noiseGateSlider.addChangeListener(e -> {
            // Convierte 10..100 a 0.010..0.100 RMS
            double sensitivity = noiseGateSlider.getValue() / 1000.0;
            config.setMicSensitivity(sensitivity);
        });

        startStopButton.addActionListener(e -> {
            if (onStartStop != null) onStartStop.run();
        });

        nextButton.addActionListener(e -> {
            if (onNext != null) onNext.run();
        });
    }

    public void setCallbacks(Runnable onStartStop, Runnable onNext, Consumer<Integer> onMaxFretChanged) {
        this.onStartStop = onStartStop;
        this.onNext = onNext;
        this.onMaxFretChanged = onMaxFretChanged;
    }

    public void updateSessionState(boolean isRunning) {
        if (isRunning) {
            startStopButton.setText("⏸ Pausar Entrenamiento");
            startStopButton.setBackground(new Color(220, 53, 69));
            nextButton.setEnabled(true);
            setOptionsEnabled(false);
        } else {
            startStopButton.setText("▶ Iniciar Entrenamiento");
            startStopButton.setBackground(new Color(40, 167, 69));
            nextButton.setEnabled(false);
            setOptionsEnabled(true);
        }
    }

    private void setOptionsEnabled(boolean enabled) {
        stringComboBox.setEnabled(enabled);
        sharpsCheckBox.setEnabled(enabled);
        timeLimitComboBox.setEnabled(enabled);
        maxFretComboBox.setEnabled(enabled);
    }
}

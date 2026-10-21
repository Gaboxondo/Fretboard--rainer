package com.threadboardlearn.ui;

import com.threadboardlearn.i18n.I18n;
import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.TrainingConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Panel de controles de configuración de niveles, cuerdas, sostenidos y micrófono.
 */
@SuppressWarnings("serial")
public class ControlPanel extends JPanel {
    private final JComboBox<String> stringComboBox = new JComboBox<>();
    private final JCheckBox sharpsCheckBox = new JCheckBox();
    private final JComboBox<String> timeLimitComboBox = new JComboBox<>();
    private final JCheckBox playAudioCheckBox = new JCheckBox();
    private final JCheckBox micEnabledCheckBox = new JCheckBox();
    private final JComboBox<String> maxFretComboBox = new JComboBox<>();

    private final JCheckBox exactOctaveCheckBox = new JCheckBox();
    private final JSlider noiseGateSlider = new JSlider(10, 100, 45); // 0.010 a 0.100 RMS

    private final JLabel strLbl = new JLabel();
    private final JLabel timeLbl = new JLabel();
    private final JLabel fretLbl = new JLabel();
    private final JLabel gateLbl = new JLabel();

    private final JButton startStopButton = new JButton();
    private final JButton nextButton = new JButton();

    private final TrainingConfig config;
    private Runnable onStartStop;
    private Runnable onNext;
    private Consumer<Integer> onMaxFretChanged;

    private boolean isRunning = false;
    private boolean updatingI18n = false;

    public ControlPanel(TrainingConfig config) {
        this.config = config;
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 75), 1, true),
                ""
        ));

        // Grid con 4 filas y 2 columnas de opciones
        JPanel optionsGrid = new JPanel(new GridLayout(4, 2, 12, 6));
        optionsGrid.setOpaque(false);
        optionsGrid.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        // Fila 1: Cuerda y Sostenidos
        JPanel stringBox = new JPanel(new BorderLayout(5, 0));
        stringBox.setOpaque(false);
        strLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        stringBox.add(strLbl, BorderLayout.WEST);
        stringBox.add(stringComboBox, BorderLayout.CENTER);
        optionsGrid.add(stringBox);
        optionsGrid.add(sharpsCheckBox);

        // Fila 2: Tiempo y Trastes
        JPanel timeBox = new JPanel(new BorderLayout(5, 0));
        timeBox.setOpaque(false);
        timeLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        timeBox.add(timeLbl, BorderLayout.WEST);
        timeBox.add(timeLimitComboBox, BorderLayout.CENTER);
        optionsGrid.add(timeBox);

        JPanel fretBox = new JPanel(new BorderLayout(5, 0));
        fretBox.setOpaque(false);
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
        gateLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
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
        updateI18n();

        // Valores por defecto
        playAudioCheckBox.setSelected(true);
        micEnabledCheckBox.setSelected(true);
        exactOctaveCheckBox.setSelected(true);
        timeLimitComboBox.setSelectedIndex(1); // 5 segundos
    }

    private void setupListeners() {
        stringComboBox.addActionListener(e -> {
            if (updatingI18n) return;
            int idx = stringComboBox.getSelectedIndex();
            if (idx <= 0) {
                config.setSelectedString(null);
            } else {
                config.setSelectedString(GuitarString.fromNumber(idx));
            }
        });

        sharpsCheckBox.addActionListener(e -> {
            if (updatingI18n) return;
            config.setIncludeSharps(sharpsCheckBox.isSelected());
        });

        timeLimitComboBox.addActionListener(e -> {
            if (updatingI18n) return;
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
            if (updatingI18n) return;
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

        playAudioCheckBox.addActionListener(e -> {
            if (updatingI18n) return;
            config.setPlayReferenceAudio(playAudioCheckBox.isSelected());
        });

        micEnabledCheckBox.addActionListener(e -> {
            if (updatingI18n) return;
            config.setMicrophoneEnabled(micEnabledCheckBox.isSelected());
        });

        exactOctaveCheckBox.addActionListener(e -> {
            if (updatingI18n) return;
            config.setMatchExactOctave(exactOctaveCheckBox.isSelected());
        });

        noiseGateSlider.addChangeListener(e -> {
            if (updatingI18n) return;
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
        this.isRunning = isRunning;
        if (isRunning) {
            startStopButton.setText(I18n.get("control.btn_pause"));
            startStopButton.setBackground(new Color(220, 53, 69));
            nextButton.setEnabled(true);
            setOptionsEnabled(false);
        } else {
            startStopButton.setText(I18n.get("control.btn_start"));
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

    public void updateI18n() {
        updatingI18n = true;
        try {
            if (getBorder() instanceof TitledBorder titledBorder) {
                titledBorder.setTitle(I18n.get("control.title"));
                repaint();
            }

            strLbl.setText(I18n.get("control.string_label"));
            sharpsCheckBox.setText(I18n.get("control.sharps"));

            timeLbl.setText(I18n.get("control.time_label"));
            fretLbl.setText(I18n.get("control.range_label"));

            playAudioCheckBox.setText(I18n.get("control.play_audio"));
            micEnabledCheckBox.setText(I18n.get("control.mic_enabled"));
            exactOctaveCheckBox.setText(I18n.get("control.exact_octave"));

            gateLbl.setText(I18n.get("control.noise_gate"));
            gateLbl.setToolTipText(I18n.get("control.noise_gate_tooltip"));
            noiseGateSlider.setToolTipText(I18n.get("control.noise_gate_slider_tooltip"));

            // Actualizar opciones de combos manteniendo la selección actual
            int selectedString = stringComboBox.getSelectedIndex();
            stringComboBox.removeAllItems();
            stringComboBox.addItem(I18n.get("control.all_strings"));
            for (GuitarString s : GuitarString.values()) {
                stringComboBox.addItem(s.getLocalizedLabel());
            }
            if (selectedString >= 0 && selectedString < stringComboBox.getItemCount()) {
                stringComboBox.setSelectedIndex(selectedString);
            } else {
                stringComboBox.setSelectedIndex(0);
            }

            int selectedTime = timeLimitComboBox.getSelectedIndex();
            timeLimitComboBox.removeAllItems();
            timeLimitComboBox.addItem(I18n.get("control.time.3s"));
            timeLimitComboBox.addItem(I18n.get("control.time.5s"));
            timeLimitComboBox.addItem(I18n.get("control.time.8s"));
            timeLimitComboBox.addItem(I18n.get("control.time.10s"));
            timeLimitComboBox.addItem(I18n.get("control.time.15s"));
            timeLimitComboBox.addItem(I18n.get("control.time.none"));
            if (selectedTime >= 0 && selectedTime < timeLimitComboBox.getItemCount()) {
                timeLimitComboBox.setSelectedIndex(selectedTime);
            } else {
                timeLimitComboBox.setSelectedIndex(1);
            }

            int selectedFret = maxFretComboBox.getSelectedIndex();
            maxFretComboBox.removeAllItems();
            maxFretComboBox.addItem(I18n.get("control.range.12"));
            maxFretComboBox.addItem(I18n.get("control.range.15"));
            maxFretComboBox.addItem(I18n.get("control.range.24"));
            if (selectedFret >= 0 && selectedFret < maxFretComboBox.getItemCount()) {
                maxFretComboBox.setSelectedIndex(selectedFret);
            } else {
                maxFretComboBox.setSelectedIndex(0);
            }

            // Actualizar texto de botones
            startStopButton.setText(isRunning ? I18n.get("control.btn_pause") : I18n.get("control.btn_start"));
            nextButton.setText(I18n.get("control.btn_next"));
        } finally {
            updatingI18n = false;
        }
    }
}

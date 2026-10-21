package com.threadboardlearn.ui;

import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.engine.TrainingStats;
import com.threadboardlearn.i18n.I18n;

import javax.swing.*;
import java.awt.*;

/**
 * Barra de estado inferior con monitor de micrófono en tiempo real (VU meter, nota detectada)
 * y estadísticas de rendimiento (racha, aciertos, precisión y tiempo de respuesta medio).
 */
@SuppressWarnings("serial")
public class StatusBar extends JPanel {
    private final JProgressBar vuMeter = new JProgressBar(0, 100);
    private final JLabel vuLbl = new JLabel();
    private final JLabel micNoteLabel = new JLabel();
    private final JLabel streakLabel = new JLabel();
    private final JLabel bestStreakLabel = new JLabel();
    private final JLabel accuracyLabel = new JLabel();
    private final JLabel avgTimeLabel = new JLabel();

    private TrainingStats lastStats;
    private PitchDetectionResult lastPitchResult;

    public StatusBar() {
        setLayout(new BorderLayout(15, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 54, 62)),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)
        ));
        setBackground(new Color(28, 30, 35));

        // Panel Izquierdo: Micrófono y VU Meter
        JPanel micPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        micPanel.setOpaque(false);

        vuLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        vuLbl.setForeground(new Color(160, 170, 185));

        vuMeter.setPreferredSize(new Dimension(80, 14));
        vuMeter.setValue(0);
        vuMeter.setForeground(new Color(46, 204, 113));

        micNoteLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        micNoteLabel.setForeground(new Color(220, 225, 235));

        micPanel.add(vuLbl);
        micPanel.add(vuMeter);
        micPanel.add(micNoteLabel);

        // Panel Derecho: Estadísticas
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        statsPanel.setOpaque(false);

        Font statsFont = new Font("SansSerif", Font.BOLD, 12);
        streakLabel.setFont(statsFont);
        streakLabel.setForeground(new Color(255, 179, 0)); // Fuego naranja

        bestStreakLabel.setFont(statsFont);
        bestStreakLabel.setForeground(new Color(241, 196, 15)); // Estrella dorada

        accuracyLabel.setFont(statsFont);
        accuracyLabel.setForeground(new Color(52, 152, 219));

        avgTimeLabel.setFont(statsFont);
        avgTimeLabel.setForeground(new Color(155, 89, 182));

        statsPanel.add(streakLabel);
        statsPanel.add(bestStreakLabel);
        statsPanel.add(accuracyLabel);
        statsPanel.add(avgTimeLabel);

        add(micPanel, BorderLayout.WEST);
        add(statsPanel, BorderLayout.EAST);

        updateI18n();
    }

    public void updatePitchResult(PitchDetectionResult result) {
        this.lastPitchResult = result;
        if (result == null) return;

        // Escalar RMS (0.0 a ~0.15) a porcentaje 0..100 para el medidor
        int vu = (int) Math.min(100, Math.round(result.rms() * 800.0));
        vuMeter.setValue(vu);

        if (vu < 10) {
            vuMeter.setForeground(new Color(46, 204, 113));
        } else if (vu < 70) {
            vuMeter.setForeground(new Color(241, 196, 15));
        } else {
            vuMeter.setForeground(new Color(231, 76, 60));
        }

        if (result.isPitched() && result.note() != null) {
            String noteStr = result.note().getName();
            double freq = result.frequencyHz();
            double cents = result.centsDeviation();
            String centsStr = (cents >= 0 ? "+" : "") + String.format(I18n.getLocale(), "%.0f", cents) + "c";
            micNoteLabel.setText(I18n.get("status.mic_heard_note", noteStr, String.format(I18n.getLocale(), "%.1f", freq), centsStr));
            micNoteLabel.setForeground(new Color(46, 204, 113));
        } else if (vu > 5) {
            micNoteLabel.setText(I18n.get("status.mic_noise"));
            micNoteLabel.setForeground(new Color(160, 165, 175));
        } else {
            micNoteLabel.setText(I18n.get("status.mic_listening"));
            micNoteLabel.setForeground(new Color(130, 135, 145));
        }
    }

    public void updateStats(TrainingStats stats) {
        this.lastStats = stats;
        if (stats == null) return;

        streakLabel.setText(I18n.get("status.streak", stats.getCurrentStreak()));
        bestStreakLabel.setText(I18n.get("status.best_streak", stats.getBestStreak()));
        accuracyLabel.setText(I18n.get("status.accuracy", stats.getCorrectCount(), stats.getTotalRounds(), stats.getAccuracyPercent()));
        if (stats.getCorrectCount() > 0) {
            avgTimeLabel.setText(I18n.get("status.avg_time", String.format(I18n.getLocale(), "%.2f", stats.getAverageReactionTimeSeconds())));
        } else {
            avgTimeLabel.setText(I18n.get("status.avg_time_empty"));
        }
    }

    public void updateI18n() {
        vuLbl.setText(I18n.get("status.mic"));

        if (lastPitchResult != null) {
            updatePitchResult(lastPitchResult);
        } else {
            micNoteLabel.setText(I18n.get("status.mic_inactive"));
        }

        if (lastStats != null) {
            updateStats(lastStats);
        } else {
            streakLabel.setText(I18n.get("status.streak", 0));
            bestStreakLabel.setText(I18n.get("status.best_streak", 0));
            accuracyLabel.setText(I18n.get("status.accuracy", 0, 0, 0));
            avgTimeLabel.setText(I18n.get("status.avg_time_empty"));
        }
    }
}

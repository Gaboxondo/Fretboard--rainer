package com.threadboardlearn.ui;

import com.threadboardlearn.dsp.PitchDetectionResult;
import com.threadboardlearn.engine.TrainingStats;

import javax.swing.*;
import java.awt.*;

/**
 * Barra de estado inferior con monitor de micrófono en tiempo real (VU meter, nota detectada)
 * y estadísticas de rendimiento (racha, aciertos, precisión y tiempo de respuesta medio).
 */
public class StatusBar extends JPanel {
    private final JProgressBar vuMeter = new JProgressBar(0, 100);
    private final JLabel micNoteLabel = new JLabel("Micrófono: Inactivo");
    private final JLabel streakLabel = new JLabel("🔥 Racha: 0");
    private final JLabel bestStreakLabel = new JLabel("⭐ Mejor: 0");
    private final JLabel accuracyLabel = new JLabel("🎯 Aciertos: 0/0 (0%)");
    private final JLabel avgTimeLabel = new JLabel("⏱ Tiempo medio: —");

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

        JLabel vuLbl = new JLabel("Mic:");
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
    }

    public void updatePitchResult(PitchDetectionResult result) {
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
            String centsStr = (cents >= 0 ? "+" : "") + String.format("%.0f", cents) + "c";
            micNoteLabel.setText(String.format("Escuchado: %s (%.1f Hz, %s)", noteStr, freq, centsStr));
            micNoteLabel.setForeground(new Color(46, 204, 113));
        } else if (vu > 5) {
            micNoteLabel.setText("Escuchado: Ruido / Sin tono");
            micNoteLabel.setForeground(new Color(160, 165, 175));
        } else {
            micNoteLabel.setText("Micrófono: Escuchando...");
            micNoteLabel.setForeground(new Color(130, 135, 145));
        }
    }

    public void updateStats(TrainingStats stats) {
        streakLabel.setText("🔥 Racha: " + stats.getCurrentStreak());
        bestStreakLabel.setText("⭐ Mejor: " + stats.getBestStreak());
        accuracyLabel.setText(String.format("🎯 Aciertos: %d/%d (%.0f%%)",
                stats.getCorrectCount(), stats.getTotalRounds(), stats.getAccuracyPercent()));
        if (stats.getCorrectCount() > 0) {
            avgTimeLabel.setText(String.format("⏱ Tiempo medio: %.2f s", stats.getAverageReactionTimeSeconds()));
        } else {
            avgTimeLabel.setText("⏱ Tiempo medio: —");
        }
    }
}

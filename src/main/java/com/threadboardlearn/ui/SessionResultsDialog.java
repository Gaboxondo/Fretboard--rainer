package com.threadboardlearn.ui;

import com.threadboardlearn.engine.AdaptiveLearningEngine;
import com.threadboardlearn.engine.NotePerformance;
import com.threadboardlearn.engine.TrainingSession;
import com.threadboardlearn.engine.TrainingStats;
import com.threadboardlearn.i18n.I18n;
import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.List;

/**
 * Diálogo modal de resultados y análisis del entrenamiento.
 * Muestra el resumen de métricas clave (aciertos, fallos, tiempo de reacción y mejor racha),
 * el gráfico interactivo de notas/cuerdas más falladas y sugerencias pedagógicas personalizadas.
 */
@SuppressWarnings("serial")
public class SessionResultsDialog extends JDialog {
    private final TrainingSession session;
    private final Runnable onResetStats;

    public SessionResultsDialog(Frame owner, TrainingSession session, Runnable onResetStats) {
        super(owner, I18n.get("results.dialog_title"), true);
        this.session = session;
        this.onResetStats = onResetStats;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        TrainingStats stats = session.getStats();
        AdaptiveLearningEngine engine = session.getAdaptiveEngine();

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(new Color(20, 22, 26));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // 1. Cabecera con Título
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);

        JLabel titleIcon = new JLabel("📊");
        titleIcon.setFont(new Font("SansSerif", Font.PLAIN, 32));

        JPanel titleTextPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titleTextPanel.setOpaque(false);

        JLabel mainTitle = new JLabel(I18n.get("results.dialog_title"));
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        mainTitle.setForeground(new Color(235, 240, 250));

        JLabel subtitle = new JLabel(I18n.get("results.dialog_subtitle", stats.getTotalRounds()));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(155, 165, 180));

        titleTextPanel.add(mainTitle);
        titleTextPanel.add(subtitle);

        headerPanel.add(titleIcon, BorderLayout.WEST);
        headerPanel.add(titleTextPanel, BorderLayout.CENTER);

        // 2. Tarjetas KPI de resumen
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiGrid.setOpaque(false);

        int totalFailures = stats.getMistakeCount() + stats.getTimeoutCount();

        kpiGrid.add(createKpiCard(
                I18n.get("results.kpi_accuracy"),
                String.format("%.0f%%", stats.getAccuracyPercent()),
                I18n.get("results.kpi_accuracy_sub", stats.getCorrectCount(), stats.getTotalRounds()),
                new Color(46, 204, 113)
        ));

        kpiGrid.add(createKpiCard(
                I18n.get("results.kpi_failures"),
                String.valueOf(totalFailures),
                I18n.get("results.kpi_failures_sub", stats.getMistakeCount(), stats.getTimeoutCount()),
                totalFailures > 0 ? new Color(231, 76, 60) : new Color(46, 204, 113)
        ));

        kpiGrid.add(createKpiCard(
                I18n.get("results.kpi_avg_time"),
                stats.getCorrectCount() > 0 ? String.format("%.2fs", stats.getAverageReactionTimeSeconds()) : "—",
                I18n.get("results.kpi_avg_time_sub"),
                new Color(155, 89, 182)
        ));

        kpiGrid.add(createKpiCard(
                I18n.get("results.kpi_streak"),
                String.valueOf(stats.getBestStreak()),
                I18n.get("results.kpi_streak_sub"),
                new Color(241, 196, 15)
        ));

        // 3. Gráfico de Fallos (Notas o Cuerdas)
        AnalyticsChartPanel chartPanel = new AnalyticsChartPanel(engine, session.getConfig());

        // 4. Tarjeta de Consejo Personalizado
        JPanel adviceCard = new JPanel(new BorderLayout(10, 0));
        adviceCard.setBackground(new Color(32, 35, 42));
        adviceCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 62, 75), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel coachIcon = new JLabel("💡");
        coachIcon.setFont(new Font("SansSerif", Font.PLAIN, 20));

        String coachText = generateAdviceText(stats, engine);
        JLabel coachLabel = new JLabel("<html>" + coachText + "</html>");
        coachLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        coachLabel.setForeground(new Color(220, 225, 235));

        adviceCard.add(coachIcon, BorderLayout.WEST);
        adviceCard.add(coachLabel, BorderLayout.CENTER);

        // 5. Botones inferiores
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonBar.setOpaque(false);

        JButton resetButton = new JButton(I18n.get("results.btn_reset"));
        resetButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> {
            if (onResetStats != null) {
                onResetStats.run();
            }
            dispose();
        });

        JButton continueButton = new JButton(I18n.get("results.btn_continue"));
        continueButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        continueButton.setBackground(new Color(40, 167, 69));
        continueButton.setForeground(Color.WHITE);
        continueButton.setFocusPainted(false);
        continueButton.addActionListener(e -> dispose());

        buttonBar.add(resetButton);
        buttonBar.add(continueButton);

        // Ensamblar secciones
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(kpiGrid, BorderLayout.NORTH);
        centerPanel.add(chartPanel, BorderLayout.CENTER);
        centerPanel.add(adviceCard, BorderLayout.SOUTH);

        root.add(headerPanel, BorderLayout.NORTH);
        root.add(centerPanel, BorderLayout.CENTER);
        root.add(buttonBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createKpiCard(String title, String value, String subtitle, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(3, 1, 2, 2));
        card.setBackground(new Color(30, 33, 40));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 55, 66), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLbl.setForeground(new Color(150, 160, 175));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        valLbl.setForeground(accentColor);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        subLbl.setForeground(new Color(130, 138, 150));

        card.add(titleLbl);
        card.add(valLbl);
        card.add(subLbl);
        return card;
    }

    private String generateAdviceText(TrainingStats stats, AdaptiveLearningEngine engine) {
        if (stats.getTotalRounds() == 0) {
            return I18n.get("results.advice_empty");
        }

        int totalFailures = stats.getMistakeCount() + stats.getTimeoutCount();
        if (totalFailures == 0 && stats.getAccuracyPercent() >= 99.0) {
            return I18n.get("results.advice_flawless");
        }

        if (session.getConfig().isAllStrings()) {
            GuitarString worst = engine.getMostFailedString();
            if (worst != null) {
                return I18n.get("results.advice_worst_string", worst.getLocalizedLabel());
            }
        } else {
            List<NotePerformance> sorted = engine.getNotesSortedByFailures(session.getAvailableNotes());
            if (!sorted.isEmpty() && (sorted.get(0).getMistakes() + sorted.get(0).getTimeouts()) > 0) {
                Note worstNote = sorted.get(0).getNote();
                String stringName = session.getConfig().getSelectedString() != null
                        ? session.getConfig().getSelectedString().getLocalizedLabel()
                        : "";
                return I18n.get("results.advice_worst_note", worstNote.getName(), stringName);
            }
        }

        if (stats.getAverageReactionTimeSeconds() > 3.0) {
            return I18n.get("results.advice_speed");
        }

        return I18n.get("results.advice_general");
    }
}

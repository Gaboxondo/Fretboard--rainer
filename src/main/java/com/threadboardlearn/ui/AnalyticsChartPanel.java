package com.threadboardlearn.ui;

import com.threadboardlearn.engine.AdaptiveLearningEngine;
import com.threadboardlearn.engine.NotePerformance;
import com.threadboardlearn.engine.StringPerformance;
import com.threadboardlearn.i18n.I18n;
import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;
import com.threadboardlearn.model.TrainingConfig;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Gráfico interactivo y moderno que renderiza el análisis de fallos:
 * - En modo 'Cuerda Única': gráfico de barras detallado de fallos por nota.
 * - En modo 'Todas las Cuerdas': gráfico comparativo de fallos por cuerda,
 *   destacando la cuerda más difícil y las notas con mayor índice de error.
 */
@SuppressWarnings("serial")
public class AnalyticsChartPanel extends JPanel {
    private final AdaptiveLearningEngine engine;
    private final TrainingConfig config;

    public AnalyticsChartPanel(AdaptiveLearningEngine engine, TrainingConfig config) {
        this.engine = engine;
        this.config = config;
        setPreferredSize(new Dimension(560, 290));
        setMinimumSize(new Dimension(480, 240));
        setBackground(new Color(24, 26, 31));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fondo redondeado con borde sutil
        g2.setColor(new Color(30, 33, 40));
        g2.fillRoundRect(8, 8, w - 16, h - 16, 12, 12);
        g2.setColor(new Color(55, 60, 72));
        g2.drawRoundRect(8, 8, w - 16, h - 16, 12, 12);

        if (config.isAllStrings()) {
            drawAllStringsChart(g2, w, h);
        } else {
            drawSingleStringChart(g2, w, h);
        }

        g2.dispose();
    }

    /**
     * Gráfico de fallos cuando se entrena en todas las cuerdas.
     */
    private void drawAllStringsChart(Graphics2D g2, int w, int h) {
        String title = I18n.get("chart.title_all_strings");
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(new Color(220, 225, 235));
        g2.drawString(title, 24, 32);

        GuitarString mostFailed = engine.getMostFailedString();
        GuitarString[] strings = GuitarString.values(); // 1ª a 6ª

        int maxFailures = 1;
        for (GuitarString s : strings) {
            StringPerformance sp = engine.getStringPerformance(s);
            if (sp != null && sp.getTotalFailures() > maxFailures) {
                maxFailures = sp.getTotalFailures();
            }
        }

        int startY = 48;
        int barHeight = 22;
        int spacing = 6;
        int maxBarWidth = w - 210;

        for (int i = 0; i < strings.length; i++) {
            GuitarString gs = strings[strings.length - 1 - i]; // De 6ª a 1ª
            StringPerformance sp = engine.getStringPerformance(gs);
            int failures = sp != null ? sp.getTotalFailures() : 0;
            int y = startY + i * (barHeight + spacing);

            boolean isWorst = (mostFailed != null && gs == mostFailed && failures > 0);

            // Etiqueta de la cuerda
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(isWorst ? new Color(255, 120, 100) : new Color(180, 185, 195));
            String strLabel = gs.getLocalizedShortLabel() + " (" + gs.getOpenNote().getName() + ")";
            g2.drawString(strLabel, 24, y + 16);

            // Barra de fondo tenue
            int barX = 85;
            g2.setColor(new Color(42, 46, 56));
            g2.fillRoundRect(barX, y + 2, maxBarWidth, barHeight - 4, 6, 6);

            // Barra rellena proporcional
            int barLen = (int) Math.round(((double) failures / maxFailures) * maxBarWidth);
            if (failures > 0) {
                barLen = Math.max(14, barLen);
                GradientPaint barGrad;
                if (isWorst) {
                    barGrad = new GradientPaint(barX, y, new Color(231, 76, 60), barX + barLen, y, new Color(192, 57, 43));
                } else {
                    barGrad = new GradientPaint(barX, y, new Color(243, 156, 18), barX + barLen, y, new Color(211, 84, 0));
                }
                g2.setPaint(barGrad);
                g2.fillRoundRect(barX, y + 2, barLen, barHeight - 4, 6, 6);
            }

            // Texto de métrica de fallos y precisión
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(230, 235, 245));
            String metric = I18n.get("chart.failures_count", failures);
            if (sp != null && sp.getAttempts() > 0) {
                metric += String.format(" (%.0f%%)", sp.getAccuracyPercent());
            }
            g2.drawString(metric, barX + maxBarWidth + 10, y + 15);

            if (isWorst) {
                g2.setColor(new Color(231, 76, 60));
                g2.drawString(I18n.get("chart.badge_most_failed"), barX + Math.min(barLen + 8, maxBarWidth - 80), y + 15);
            }
        }

        // Resumen inferior de notas más falladas globalmente
        List<NotePerformance> topNotes = engine.getNotesSortedByFailures(null).stream()
                .filter(np -> (np.getMistakes() + np.getTimeouts()) > 0)
                .limit(3)
                .toList();

        if (!topNotes.isEmpty()) {
            int bottomY = h - 22;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(160, 168, 182));
            StringBuilder sb = new StringBuilder(I18n.get("chart.top_failed_notes") + " ");
            for (int i = 0; i < topNotes.size(); i++) {
                NotePerformance np = topNotes.get(i);
                sb.append(np.getNote().getName())
                        .append(" (")
                        .append(np.getMistakes() + np.getTimeouts())
                        .append(")");
                if (i < topNotes.size() - 1) sb.append(", ");
            }
            g2.drawString(sb.toString(), 24, bottomY);
        }
    }

    /**
     * Gráfico de fallos cuando se entrena en una cuerda específica.
     */
    private void drawSingleStringChart(Graphics2D g2, int w, int h) {
        String stringName = config.getSelectedString() != null ? config.getSelectedString().getLocalizedLabel() : "";
        String title = I18n.get("chart.title_single_string", stringName);

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(new Color(220, 225, 235));
        g2.drawString(title, 24, 32);

        List<Note> activeNotes = config.isIncludeSharps() ? Note.getAllNotes() : Note.getNaturalNotes();
        List<NotePerformance> sorted = engine.getNotesSortedByFailures(activeNotes);

        int maxFailures = 1;
        for (NotePerformance np : sorted) {
            int fails = np.getMistakes() + np.getTimeouts();
            if (fails > maxFailures) {
                maxFailures = fails;
            }
        }

        int startY = 46;
        int barHeight = 20;
        int spacing = 5;
        int maxBarWidth = w - 210;
        int maxItemsToShow = Math.min(7, sorted.size());

        for (int i = 0; i < maxItemsToShow; i++) {
            NotePerformance np = sorted.get(i);
            int fails = np.getMistakes() + np.getTimeouts();
            int y = startY + i * (barHeight + spacing);

            boolean isWorst = (i == 0 && fails > 0);

            // Insignia de la nota
            g2.setColor(isWorst ? new Color(231, 76, 60, 50) : new Color(52, 152, 219, 40));
            g2.fillRoundRect(24, y + 1, 32, barHeight - 2, 4, 4);
            g2.setColor(isWorst ? new Color(255, 120, 100) : new Color(241, 196, 15));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(np.getNote().getName(), 30, y + 14);

            // Barra base
            int barX = 70;
            g2.setColor(new Color(42, 46, 56));
            g2.fillRoundRect(barX, y + 2, maxBarWidth, barHeight - 4, 6, 6);

            // Barra rellena
            int barLen = (int) Math.round(((double) fails / maxFailures) * maxBarWidth);
            if (fails > 0) {
                barLen = Math.max(14, barLen);
                GradientPaint barGrad;
                if (isWorst) {
                    barGrad = new GradientPaint(barX, y, new Color(231, 76, 60), barX + barLen, y, new Color(192, 57, 43));
                } else {
                    barGrad = new GradientPaint(barX, y, new Color(243, 156, 18), barX + barLen, y, new Color(211, 84, 0));
                }
                g2.setPaint(barGrad);
                g2.fillRoundRect(barX, y + 2, barLen, barHeight - 4, 6, 6);
            }

            // Métrica de fallos y tiempo de reacción
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(230, 235, 245));
            String metric = I18n.get("chart.failures_count", fails);
            if (np.getAttempts() > 0) {
                metric += String.format(" • %.1fs", np.getAverageReactionTimeSeconds());
            }
            g2.drawString(metric, barX + maxBarWidth + 10, y + 14);
        }

        // Pie explicativo
        int bottomY = h - 18;
        g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
        g2.setColor(new Color(150, 158, 170));
        g2.drawString(I18n.get("chart.footer_hint"), 24, bottomY);
    }
}

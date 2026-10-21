package com.threadboardlearn.ui;

import com.threadboardlearn.engine.Challenge;
import com.threadboardlearn.i18n.I18n;

import javax.swing.*;
import java.awt.*;

/**
 * Panel superior con la presentación visual del reto actual:
 * Nota objetivo en grande, cuerda donde buscarla, barra de tiempo regresiva y feedback.
 */
@SuppressWarnings("serial")
public class ChallengeDisplayPanel extends JPanel {
    private enum State { INITIAL, IN_PROGRESS, SUCCESS, TIMEOUT }

    private final JLabel noteLabel = new JLabel("—", SwingConstants.CENTER);
    private final JLabel stringLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JProgressBar timerProgressBar = new JProgressBar(0, 1000);
    private final JButton replaySoundButton = new JButton();

    private Runnable onReplayAudio;
    private Challenge currentChallenge;
    private State currentState = State.INITIAL;
    private long lastReactionTimeMs = 0;
    private int lastSecondsRemaining = 0;
    private double lastPercentElapsed = 0.0;

    public ChallengeDisplayPanel() {
        setLayout(new BorderLayout(15, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 1. Tarjeta central de la Nota
        JPanel noteCard = new JPanel(new BorderLayout(5, 5));
        noteCard.setBackground(new Color(35, 38, 44));
        noteCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 75), 1, true),
                BorderFactory.createEmptyBorder(10, 25, 10, 25)));

        noteLabel.setFont(new Font("SansSerif", Font.BOLD, 68));
        noteLabel.setForeground(new Color(255, 193, 7)); // Color dorado ámbar

        stringLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        stringLabel.setForeground(new Color(225, 230, 240));

        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        statusLabel.setForeground(new Color(150, 160, 175));

        noteCard.add(noteLabel, BorderLayout.CENTER);

        JPanel infoSubPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        infoSubPanel.setOpaque(false);
        infoSubPanel.add(stringLabel);
        infoSubPanel.add(statusLabel);
        noteCard.add(infoSubPanel, BorderLayout.SOUTH);

        // 2. Barra de tiempo y controles
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setOpaque(false);

        timerProgressBar.setValue(1000);
        timerProgressBar.setStringPainted(true);
        timerProgressBar.setForeground(new Color(46, 204, 113));
        timerProgressBar.setPreferredSize(new Dimension(300, 20));

        replaySoundButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        replaySoundButton.setFocusPainted(false);
        replaySoundButton.addActionListener(e -> {
            if (onReplayAudio != null) {
                onReplayAudio.run();
            }
        });

        bottomPanel.add(timerProgressBar, BorderLayout.CENTER);
        bottomPanel.add(replaySoundButton, BorderLayout.EAST);

        add(noteCard, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        updateI18n();
    }

    public void setOnReplayAudio(Runnable onReplayAudio) {
        this.onReplayAudio = onReplayAudio;
    }

    public void showChallenge(Challenge challenge) {
        this.currentChallenge = challenge;
        this.currentState = State.IN_PROGRESS;

        noteLabel.setText(challenge.targetNote().getName());
        noteLabel.setForeground(new Color(255, 193, 7));
        stringLabel.setText(I18n.get("challenge.find_on", challenge.targetString().getLocalizedLabel()));
        statusLabel.setText(I18n.get("challenge.instruction"));
        statusLabel.setForeground(new Color(170, 180, 195));
        replaySoundButton.setEnabled(true);
    }

    public void updateTimer(int secondsRemaining, double percentElapsed) {
        this.lastSecondsRemaining = secondsRemaining;
        this.lastPercentElapsed = percentElapsed;

        int progressValue = (int) Math.round((1.0 - percentElapsed) * 1000.0);
        timerProgressBar.setValue(progressValue);

        if (secondsRemaining > 0) {
            timerProgressBar.setString(I18n.get("challenge.timer_remaining", secondsRemaining));
        } else {
            timerProgressBar.setString(I18n.get("challenge.timer_no_limit"));
            timerProgressBar.setValue(1000);
        }

        // Cambio de color según el tiempo que va quedando
        if (percentElapsed < 0.55) {
            timerProgressBar.setForeground(new Color(46, 204, 113)); // Verde
        } else if (percentElapsed < 0.8) {
            timerProgressBar.setForeground(new Color(243, 156, 18)); // Amarillo
        } else {
            timerProgressBar.setForeground(new Color(231, 76, 60)); // Rojo
        }
    }

    public void showSuccess(long reactionTimeMs) {
        this.currentState = State.SUCCESS;
        this.lastReactionTimeMs = reactionTimeMs;

        double seconds = reactionTimeMs / 1000.0;
        noteLabel.setForeground(new Color(46, 204, 113));
        statusLabel.setText(I18n.get("challenge.success", String.format(I18n.getLocale(), "%.2f", seconds)));
        statusLabel.setForeground(new Color(46, 204, 113));
        timerProgressBar.setValue(1000);
        timerProgressBar.setString(I18n.get("challenge.timer_correct"));
        timerProgressBar.setForeground(new Color(46, 204, 113));
    }

    public void showTimeout(Challenge challenge) {
        this.currentChallenge = challenge;
        this.currentState = State.TIMEOUT;

        noteLabel.setForeground(new Color(231, 76, 60));
        String fretsStr = challenge.validFrets().toString();
        statusLabel.setText(I18n.get("challenge.timeout", fretsStr));
        statusLabel.setForeground(new Color(231, 76, 60));
        timerProgressBar.setValue(0);
        timerProgressBar.setString(I18n.get("challenge.timer_timeout"));
        timerProgressBar.setForeground(new Color(231, 76, 60));
    }

    public void reset() {
        this.currentState = State.INITIAL;
        this.currentChallenge = null;

        noteLabel.setText("—");
        noteLabel.setForeground(new Color(255, 193, 7));
        stringLabel.setText(I18n.get("challenge.reset_instruction"));
        statusLabel.setText(" ");
        timerProgressBar.setValue(1000);
        timerProgressBar.setString(I18n.get("challenge.timer_ready"));
        replaySoundButton.setEnabled(false);
    }

    public void updateI18n() {
        replaySoundButton.setText(I18n.get("challenge.hear_note"));

        switch (currentState) {
            case INITIAL -> {
                stringLabel.setText(I18n.get("challenge.initial_instruction"));
                timerProgressBar.setString(I18n.get("challenge.timer_ready"));
            }
            case IN_PROGRESS -> {
                if (currentChallenge != null) {
                    stringLabel.setText(I18n.get("challenge.find_on", currentChallenge.targetString().getLocalizedLabel()));
                    statusLabel.setText(I18n.get("challenge.instruction"));
                    updateTimer(lastSecondsRemaining, lastPercentElapsed);
                }
            }
            case SUCCESS -> showSuccess(lastReactionTimeMs);
            case TIMEOUT -> {
                if (currentChallenge != null) {
                    showTimeout(currentChallenge);
                }
            }
        }
    }
}

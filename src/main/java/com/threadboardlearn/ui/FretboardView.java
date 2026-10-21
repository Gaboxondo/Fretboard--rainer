package com.threadboardlearn.ui;

import com.threadboardlearn.engine.Challenge;
import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Componente gráfico interactivo que dibuja el diapasón de la guitarra con estilo premium.
 * Soporta clics interactivos para responder a los retos, marcadores de trastes (inlays),
 * grosor de cuerdas proporcional y animaciones de acierto/fallo.
 */
@SuppressWarnings("serial")
public class FretboardView extends JPanel {
    private static final int DEFAULT_MAX_FRET = 12;
    private int maxFret = DEFAULT_MAX_FRET;

    // Fret markers estándar de guitarra
    private static final Set<Integer> SINGLE_DOT_FRETS = Set.of(3, 5, 7, 9, 15, 17, 19, 21);
    private static final Set<Integer> DOUBLE_DOT_FRETS = Set.of(12, 24);

    private Challenge currentChallenge;
    private BiConsumer<GuitarString, Integer> onFretClicked;

    // Estado visual de feedback
    private boolean showSolution = false;
    private boolean successFeedback = false;
    private final Set<Point> highlightedFrets = new HashSet<>(); // Point(stringNumber, fret)

    // Paleta de colores cálidos y oscuros estilo madera / estudio
    private static final Color FRETBOARD_WOOD_TOP = new Color(44, 30, 22);
    private static final Color FRETBOARD_WOOD_BOTTOM = new Color(28, 18, 12);
    private static final Color NUT_COLOR = new Color(230, 225, 210);
    private static final Color FRET_WIRE_COLOR = new Color(195, 195, 205);
    private static final Color FRET_SHADOW_COLOR = new Color(15, 10, 8);
    private static final Color INLAY_DOT_COLOR = new Color(215, 210, 195, 200);
    private static final Color STRING_WOUND_COLOR = new Color(205, 180, 140);
    private static final Color STRING_PLAIN_COLOR = new Color(220, 225, 230);
    private static final Color ACTIVE_STRING_HIGHLIGHT = new Color(255, 193, 7, 45);

    public FretboardView() {
        setPreferredSize(new Dimension(980, 260));
        setMinimumSize(new Dimension(750, 220));
        setBackground(new Color(24, 25, 28));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    public void setOnFretClicked(BiConsumer<GuitarString, Integer> onFretClicked) {
        this.onFretClicked = onFretClicked;
    }

    public void setMaxFret(int maxFret) {
        this.maxFret = maxFret;
        repaint();
    }

    public void setChallenge(Challenge challenge) {
        this.currentChallenge = challenge;
        this.showSolution = false;
        this.successFeedback = false;
        this.highlightedFrets.clear();
        repaint();
    }

    public void showSuccessFeedback(Challenge challenge) {
        this.currentChallenge = challenge;
        this.successFeedback = true;
        this.showSolution = true;
        this.highlightedFrets.clear();
        if (challenge != null) {
            for (int fret : challenge.validFrets()) {
                highlightedFrets.add(new Point(challenge.targetString().getNumber(), fret));
            }
        }
        repaint();
    }

    public void showTimeoutSolution(Challenge challenge) {
        this.currentChallenge = challenge;
        this.successFeedback = false;
        this.showSolution = true;
        this.highlightedFrets.clear();
        if (challenge != null) {
            for (int fret : challenge.validFrets()) {
                highlightedFrets.add(new Point(challenge.targetString().getNumber(), fret));
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int leftMargin = 75; // Espacio para nombres de cuerdas y cejuela
        int rightMargin = 30;
        int topMargin = 35;
        int bottomMargin = 40;

        int fretboardWidth = width - leftMargin - rightMargin;
        int fretboardHeight = height - topMargin - bottomMargin;

        if (fretboardWidth <= 0 || fretboardHeight <= 0) {
            g2.dispose();
            return;
        }

        // 1. Dibujar madera del diapasón con degradado elegante
        GradientPaint woodGradient = new GradientPaint(
                leftMargin, topMargin, FRETBOARD_WOOD_TOP,
                leftMargin, topMargin + fretboardHeight, FRETBOARD_WOOD_BOTTOM
        );
        g2.setPaint(woodGradient);
        g2.fillRoundRect(leftMargin, topMargin, fretboardWidth, fretboardHeight, 6, 6);

        // Borde fino del mástil
        g2.setColor(new Color(80, 55, 40));
        g2.drawRoundRect(leftMargin, topMargin, fretboardWidth, fretboardHeight, 6, 6);

        // 2. Calcular posiciones X de los trastes (escala logarítmica / decreciente natural de guitarra)
        int[] fretX = calculateFretPositions(leftMargin, fretboardWidth, maxFret);

        // 3. Dibujar Cejuela (Nut / Traste 0)
        int nutWidth = 10;
        g2.setColor(NUT_COLOR);
        g2.fillRoundRect(leftMargin - nutWidth, topMargin - 2, nutWidth, fretboardHeight + 4, 3, 3);
        g2.setColor(new Color(170, 165, 150));
        g2.drawRoundRect(leftMargin - nutWidth, topMargin - 2, nutWidth, fretboardHeight + 4, 3, 3);

        // 4. Marcadores de posición (Inlay Dots)
        drawInlays(g2, fretX, topMargin, fretboardHeight);

        // 5. Dibujar Trastes (Fret Wires metálicos)
        for (int i = 1; i <= maxFret; i++) {
            int fx = fretX[i];
            // Sombra del traste
            g2.setColor(FRET_SHADOW_COLOR);
            g2.drawLine(fx + 1, topMargin, fx + 1, topMargin + fretboardHeight);
            // Alambre metálico del traste
            g2.setColor(FRET_WIRE_COLOR);
            g2.setStroke(new BasicStroke(i == 0 ? 3.0f : 2.0f));
            g2.drawLine(fx, topMargin, fx, topMargin + fretboardHeight);

            // Número de traste debajo
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(new Color(150, 155, 165));
            int prevX = fretX[i - 1];
            int centerFretX = (prevX + fx) / 2;
            String fretStr = String.valueOf(i);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(fretStr, centerFretX - fm.stringWidth(fretStr) / 2, topMargin + fretboardHeight + 22);
        }

        // Traste 0 indicador
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(130, 135, 145));
        g2.drawString("0 (Aire)", leftMargin - 52, topMargin - 12);

        // 6. Resaltar la cuerda activa si hay un reto en curso
        double stringSpacing = (double) fretboardHeight / 5.0;
        if (currentChallenge != null && currentChallenge.targetString() != null) {
            int stringIdx = currentChallenge.targetString().getNumber() - 1; // 0 = cuerda 1, 5 = cuerda 6
            int sy = (int) Math.round(topMargin + stringIdx * stringSpacing);
            g2.setColor(ACTIVE_STRING_HIGHLIGHT);
            g2.fillRoundRect(leftMargin, sy - 14, fretboardWidth, 28, 8, 8);
        }

        // 7. Dibujar las 6 cuerdas con grosor y textura
        for (int s = 0; s < 6; s++) {
            GuitarString gString = GuitarString.values()[s];
            int sy = (int) Math.round(topMargin + s * stringSpacing);

            // Etiqueta de la cuerda a la izquierda
            drawGuitarStringLabel(g2, gString, leftMargin - nutWidth - 8, sy);

            // Dibujar la cuerda
            float gauge = gString.getVisualGauge();
            Color stringColor = (s >= 2) ? STRING_WOUND_COLOR : STRING_PLAIN_COLOR;

            // Sombra de la cuerda sobre la madera
            g2.setColor(new Color(10, 8, 5, 140));
            g2.setStroke(new BasicStroke(gauge + 1.0f));
            g2.drawLine(leftMargin - nutWidth, sy + 1, leftMargin + fretboardWidth, sy + 1);

            // Cuerda brillante
            g2.setColor(stringColor);
            g2.setStroke(new BasicStroke(gauge, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(leftMargin - nutWidth, sy, leftMargin + fretboardWidth, sy);
        }

        // 8. Dibujar notas resaltadas (solución o acierto)
        if (showSolution) {
            drawSolutionMarkers(g2, fretX, leftMargin, topMargin, stringSpacing);
        }

        g2.dispose();
    }

    private void drawGuitarStringLabel(Graphics2D g2, GuitarString gString, int xRight, int yCenter) {
        boolean isTarget = (currentChallenge != null && currentChallenge.targetString() == gString);

        g2.setFont(new Font("SansSerif", isTarget ? Font.BOLD : Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();

        String numStr = gString.getNumber() + "ª";
        String noteStr = gString.getOpenNote().getName();
        String fullStr = numStr + " " + noteStr;

        int strWidth = fm.stringWidth(fullStr);
        int textX = xRight - strWidth - 6;
        int textY = yCenter + fm.getAscent() / 2 - 2;

        if (isTarget) {
            // Insignia brillante dorada/ámbar
            g2.setColor(new Color(255, 193, 7, 220));
            g2.fillRoundRect(textX - 5, yCenter - 11, strWidth + 10, 22, 6, 6);
            g2.setColor(Color.BLACK);
            g2.drawString(fullStr, textX, textY);
        } else {
            g2.setColor(new Color(180, 185, 195));
            g2.drawString(fullStr, textX, textY);
        }
    }

    private void drawInlays(Graphics2D g2, int[] fretX, int topMargin, int height) {
        int centerY = topMargin + height / 2;
        int dotDiameter = 12;

        for (int f = 1; f <= maxFret; f++) {
            int prevX = fretX[f - 1];
            int currX = fretX[f];
            int midX = (prevX + currX) / 2;

            if (SINGLE_DOT_FRETS.contains(f)) {
                g2.setColor(INLAY_DOT_COLOR);
                g2.fillOval(midX - dotDiameter / 2, centerY - dotDiameter / 2, dotDiameter, dotDiameter);
            } else if (DOUBLE_DOT_FRETS.contains(f)) {
                int offset = height / 4;
                g2.setColor(INLAY_DOT_COLOR);
                g2.fillOval(midX - dotDiameter / 2, centerY - offset - dotDiameter / 2, dotDiameter, dotDiameter);
                g2.fillOval(midX - dotDiameter / 2, centerY + offset - dotDiameter / 2, dotDiameter, dotDiameter);
            }
        }
    }

    private void drawSolutionMarkers(Graphics2D g2, int[] fretX, int leftMargin, int topMargin, double stringSpacing) {
        Color badgeColor = successFeedback ? new Color(46, 204, 113) : new Color(231, 76, 60);
        Color textColor = Color.WHITE;

        for (Point p : highlightedFrets) {
            int stringNum = p.x;
            int fret = p.y;

            int stringIdx = stringNum - 1;
            int cy = (int) Math.round(topMargin + stringIdx * stringSpacing);

            int cx;
            if (fret == 0) {
                cx = leftMargin - 20;
            } else {
                int prevX = fretX[fret - 1];
                int currX = fretX[fret];
                cx = (prevX + currX) / 2;
            }

            int radius = 15;
            // Glow exterior
            g2.setColor(new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 80));
            g2.fillOval(cx - radius - 5, cy - radius - 5, (radius + 5) * 2, (radius + 5) * 2);

            // Círculo sólido de la nota
            g2.setColor(badgeColor);
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // Texto de la nota dentro del círculo
            if (currentChallenge != null) {
                String noteName = currentChallenge.targetNote().getName();
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth(noteName) / 2;
                int ty = cy + fm.getAscent() / 2 - 2;
                g2.setColor(textColor);
                g2.drawString(noteName, tx, ty);
            }
        }
    }

    private int[] calculateFretPositions(int leftMargin, int width, int frets) {
        int[] positions = new int[frets + 1];
        positions[0] = leftMargin;

        // Distribución física realista con factor de escala 2^(1/12)
        double totalScaleLength = width / (1.0 - Math.pow(2.0, -frets / 12.0));
        for (int i = 1; i <= frets; i++) {
            double distance = totalScaleLength * (1.0 - Math.pow(2.0, -i / 12.0));
            positions[i] = leftMargin + (int) Math.round(distance);
        }
        return positions;
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        if (onFretClicked == null) return;

        int leftMargin = 75;
        int rightMargin = 30;
        int topMargin = 35;
        int bottomMargin = 40;

        int fretboardWidth = getWidth() - leftMargin - rightMargin;
        int fretboardHeight = getHeight() - topMargin - bottomMargin;

        if (fretboardWidth <= 0 || fretboardHeight <= 0) return;

        double stringSpacing = (double) fretboardHeight / 5.0;

        // 1. Detectar cuerda más cercana
        int closestStringNum = -1;
        double minDistance = Double.MAX_VALUE;

        for (int s = 0; s < 6; s++) {
            int sy = (int) Math.round(topMargin + s * stringSpacing);
            double dist = Math.abs(mouseY - sy);
            if (dist < minDistance && dist <= stringSpacing * 0.75) {
                minDistance = dist;
                closestStringNum = s + 1;
            }
        }

        if (closestStringNum == -1) return;

        // 2. Detectar traste
        int[] fretX = calculateFretPositions(leftMargin, fretboardWidth, maxFret);
        int detectedFret = -1;

        if (mouseX < leftMargin && mouseX >= leftMargin - 45) {
            detectedFret = 0; // Cuerda al aire
        } else {
            for (int f = 1; f <= maxFret; f++) {
                int prev = fretX[f - 1];
                int curr = fretX[f];
                if (mouseX >= prev && mouseX <= curr) {
                    detectedFret = f;
                    break;
                }
            }
        }

        if (detectedFret != -1) {
            GuitarString gString = GuitarString.fromNumber(closestStringNum);
            onFretClicked.accept(gString, detectedFret);
        }
    }
}

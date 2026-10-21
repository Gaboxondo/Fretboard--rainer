package com.threadboardlearn;

import com.formdev.flatlaf.FlatDarkLaf;
import com.threadboardlearn.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada principal de la aplicación Guitar Fretboard Trainer.
 */
public class App {
    public static void main(String[] args) {
        // Configurar propiedades de renderizado de texto de alta definición
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Inicializar tema moderno oscuro con FlatLaf
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            System.err.println("No se pudo cargar FlatDarkLaf, usando tema del sistema: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Iniciar la interfaz en el hilo de despacho de eventos de Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

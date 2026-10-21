package com.threadboardlearn.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestor central de internacionalización (i18n) para la aplicación.
 * Permite cambiar de idioma dinámicamente y notificar a los componentes de la interfaz.
 */
public final class I18n {
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static Language currentLanguage = Language.SPANISH;
    private static ResourceBundle currentBundle;
    private static final List<LanguageChangeListener> listeners = new CopyOnWriteArrayList<>();

    static {
        loadBundle();
    }

    private I18n() {
        // Utility class
    }

    /**
     * Interfaz de callback para escuchar cambios de idioma.
     */
    @FunctionalInterface
    public interface LanguageChangeListener {
        void onLanguageChanged(Language newLanguage);
    }

    /**
     * Cambia el idioma activo de la aplicación y notifica a todos los escuchadores registrados.
     */
    public static synchronized void setLanguage(Language language) {
        if (language == null || language == currentLanguage) {
            return;
        }
        currentLanguage = language;
        loadBundle();
        for (LanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(currentLanguage);
            } catch (Exception e) {
                System.err.println("Error notificando cambio de idioma: " + e.getMessage());
            }
        }
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static Locale getLocale() {
        return currentLanguage.getLocale();
    }

    public static void addListener(LanguageChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Obtiene el texto traducido para una clave dada.
     */
    public static String get(String key) {
        try {
            if (currentBundle != null && currentBundle.containsKey(key)) {
                return currentBundle.getString(key);
            }
        } catch (MissingResourceException ignored) {
        }
        return "!" + key + "!";
    }

    /**
     * Obtiene el texto traducido formateando argumentos con MessageFormat.
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            MessageFormat formatter = new MessageFormat(pattern, currentLanguage.getLocale());
            return formatter.format(args);
        } catch (Exception e) {
            return pattern;
        }
    }

    private static void loadBundle() {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLanguage.getLocale(),
                    ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        } catch (MissingResourceException e) {
            System.err.println("No se encontró el ResourceBundle para " + currentLanguage + ": " + e.getMessage());
        }
    }
}

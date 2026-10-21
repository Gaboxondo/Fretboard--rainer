package com.threadboardlearn.i18n;

import lombok.Getter;

import java.util.Locale;

/**
 * Idiomas soportados por la aplicación.
 */
@Getter
public enum Language {
    SPANISH("es", "Español", Locale.of("es")),
    ENGLISH("en", "English", Locale.ENGLISH);

    private final String code;
    private final String displayName;
    private final Locale locale;

    Language(String code, String displayName, Locale locale) {
        this.code = code;
        this.displayName = displayName;
        this.locale = locale;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return SPANISH;
    }
}

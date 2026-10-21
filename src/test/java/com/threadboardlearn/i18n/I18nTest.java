package com.threadboardlearn.i18n;

import com.threadboardlearn.model.GuitarString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    @AfterEach
    void resetLanguage() {
        I18n.setLanguage(Language.SPANISH);
    }

    @Test
    @DisplayName("Debe devolver traducciones en español por defecto")
    void testDefaultSpanishTranslations() {
        I18n.setLanguage(Language.SPANISH);
        assertThat(I18n.getLanguage()).isEqualTo(Language.SPANISH);
        assertThat(I18n.get("app.title")).contains("Aprende las Notas");
        assertThat(I18n.get("control.btn_start")).contains("Iniciar Entrenamiento");
        assertThat(I18n.get("challenge.hear_note")).contains("Escuchar nota");
    }

    @Test
    @DisplayName("Debe cambiar correctamente al inglés y reflejar las traducciones")
    void testEnglishTranslations() {
        I18n.setLanguage(Language.ENGLISH);
        assertThat(I18n.getLanguage()).isEqualTo(Language.ENGLISH);
        assertThat(I18n.get("app.title")).contains("Learn Guitar Notes");
        assertThat(I18n.get("control.btn_start")).contains("Start Training");
        assertThat(I18n.get("challenge.hear_note")).contains("Hear note");
    }

    @Test
    @DisplayName("Debe formatear argumentos correctamente con MessageFormat")
    void testFormattedStrings() {
        I18n.setLanguage(Language.SPANISH);
        assertThat(I18n.get("challenge.find_on", "6ª Cuerda"))
                .isEqualTo("Busca la nota en: 6ª Cuerda");
        assertThat(I18n.get("status.streak", 5))
                .isEqualTo("🔥 Racha: 5");

        I18n.setLanguage(Language.ENGLISH);
        assertThat(I18n.get("challenge.find_on", "6th String"))
                .isEqualTo("Find note on: 6th String");
        assertThat(I18n.get("status.streak", 5))
                .isEqualTo("🔥 Streak: 5");
    }

    @Test
    @DisplayName("Las etiquetas de cuerdas deben traducirse al cambiar de idioma")
    void testGuitarStringLocalization() {
        I18n.setLanguage(Language.SPANISH);
        assertThat(GuitarString.STRING_1.getLocalizedLabel()).contains("1ª Cuerda");
        assertThat(GuitarString.STRING_6.getLocalizedLabel()).contains("6ª Cuerda");

        I18n.setLanguage(Language.ENGLISH);
        assertThat(GuitarString.STRING_1.getLocalizedLabel()).contains("1st String");
        assertThat(GuitarString.STRING_6.getLocalizedLabel()).contains("6th String");
    }

    @Test
    @DisplayName("Los escuchadores deben ser notificados al cambiar de idioma")
    void testLanguageChangeListener() {
        AtomicBoolean notified = new AtomicBoolean(false);
        I18n.LanguageChangeListener listener = lang -> {
            if (lang == Language.ENGLISH) {
                notified.set(true);
            }
        };

        I18n.addListener(listener);
        try {
            I18n.setLanguage(Language.ENGLISH);
            assertThat(notified.get()).isTrue();
        } finally {
            I18n.removeListener(listener);
        }
    }

    @Test
    @DisplayName("Todos los bundles (Español e Inglés) deben tener exactamente las mismas claves")
    void testBundleParity() {
        ResourceBundle esBundle = ResourceBundle.getBundle("i18n.messages", Language.SPANISH.getLocale());
        ResourceBundle enBundle = ResourceBundle.getBundle("i18n.messages", Language.ENGLISH.getLocale());

        Set<String> esKeys = getKeysAsSet(esBundle);
        Set<String> enKeys = getKeysAsSet(enBundle);

        assertThat(esKeys)
                .as("Claves en Español pero no en Inglés")
                .containsExactlyInAnyOrderElementsOf(enKeys);
    }

    private Set<String> getKeysAsSet(ResourceBundle bundle) {
        Set<String> keys = new HashSet<>();
        Enumeration<String> enumeration = bundle.getKeys();
        while (enumeration.hasMoreElements()) {
            keys.add(enumeration.nextElement());
        }
        return keys;
    }
}

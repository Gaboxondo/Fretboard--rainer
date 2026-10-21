package com.threadboardlearn.engine;

import com.threadboardlearn.model.GuitarString;
import com.threadboardlearn.model.Note;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeTest {

    @Test
    @DisplayName("Debe detectar B en la 6ª cuerda (traste 7) tanto en fundamental como en 2º armónico")
    void testDetectBOnSixthString() {
        // Reto: Nota B en la 6ª cuerda (E). Traste 7 = B2 (~123.47 Hz)
        List<Integer> frets = GuitarString.STRING_6.findFretsForNote(Note.B, 12);
        Challenge challenge = new Challenge(Note.B, GuitarString.STRING_6, frets, System.currentTimeMillis());

        assertThat(frets).containsExactly(7);

        double fundamentalB2 = 123.47;
        double harmonicB3 = fundamentalB2 * 2.0; // 246.94 Hz

        // Fundamental B2 dentro de ±35 cents
        assertThat(challenge.matchesStringFrequency(fundamentalB2, 35.0)).isTrue();
        assertThat(challenge.matchesStringFrequency(124.0, 35.0)).isTrue();

        // 2º armónico B3 (octava superior capturada por micrófonos con filtro paso-alto)
        assertThat(challenge.matchesStringFrequency(harmonicB3, 35.0)).isTrue();
        assertThat(challenge.matchesStringFrequency(248.0, 35.0)).isTrue();

        // Otra nota (ej. C3 a 130.81 Hz o A2 a 110 Hz) no debe coincidir
        assertThat(challenge.matchesStringFrequency(130.81, 35.0)).isFalse();
        assertThat(challenge.matchesStringFrequency(110.0, 35.0)).isFalse();
    }

    @Test
    @DisplayName("No debe aceptar el traste anterior (C# en traste 9) al buscar D (traste 10) en la 6ª cuerda")
    void testRejectAdjacentFretForD() {
        // Reto: Nota D en la 6ª cuerda. Traste 10 = D3 (~146.83 Hz)
        List<Integer> frets = GuitarString.STRING_6.findFretsForNote(Note.D, 12);
        Challenge challenge = new Challenge(Note.D, GuitarString.STRING_6, frets, System.currentTimeMillis());

        assertThat(frets).containsExactly(10);

        double targetD3 = 146.83;
        double fret9CSharp3 = 138.59; // C#3 en traste 9 (100 cents abajo)

        // D3 coincide
        assertThat(challenge.matchesStringFrequency(targetD3, 35.0)).isTrue();

        // C#3 en traste 9 está a 100 cents y NO debe coincidir con tolerancia de 35 cents
        assertThat(challenge.matchesStringFrequency(fret9CSharp3, 35.0)).isFalse();

        // Incluso con traste 9 ligeramente alto (+15 cents de pulsación fuerte = 85 cents de diferencia)
        double sharpFret9 = 138.59 * Math.pow(2.0, 15.0 / 1200.0);
        assertThat(challenge.matchesStringFrequency(sharpFret9, 35.0)).isFalse();
    }
}

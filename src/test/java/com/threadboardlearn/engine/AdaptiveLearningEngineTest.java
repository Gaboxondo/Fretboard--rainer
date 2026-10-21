package com.threadboardlearn.engine;

import com.threadboardlearn.model.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AdaptiveLearningEngineTest {

    private AdaptiveLearningEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AdaptiveLearningEngine();
    }

    @Test
    @DisplayName("Inicialmente todas las notas deben tener peso base 1.0 y probabilidad uniforme")
    void testInitialUniformProbabilities() {
        List<Note> naturalNotes = Note.getNaturalNotes();
        double expectedProb = 1.0 / naturalNotes.size();

        for (Note note : naturalNotes) {
            assertThat(engine.getWeight(note)).isEqualTo(1.0);
            assertThat(engine.getProbability(note, naturalNotes))
                    .isCloseTo(expectedProb, within(0.0001));
        }
    }

    @Test
    @DisplayName("Fallar una nota mediante clics erróneos debe aumentar su peso y probabilidad")
    void testMistakesIncreaseProbability() {
        List<Note> naturalNotes = Note.getNaturalNotes();
        Note target = Note.F;

        // Registrar 3 fallos en la nota F
        engine.recordMistake(target);
        engine.recordMistake(target);
        engine.recordMistake(target);

        assertThat(engine.getWeight(target)).isGreaterThan(1.0);
        assertThat(engine.getProbability(target, naturalNotes))
                .isGreaterThan(engine.getProbability(Note.C, naturalNotes));

        // Debe considerarse nota foco
        assertThat(engine.isFocusNote(target, naturalNotes)).isTrue();
        assertThat(engine.getMostChallengingNote(naturalNotes)).isEqualTo(target);
    }

    @Test
    @DisplayName("El tiempo agotado (timeout) debe penalizar fuertemente e incrementar la probabilidad")
    void testTimeoutIncreasesWeightSignificantly() {
        List<Note> naturalNotes = Note.getNaturalNotes();
        Note target = Note.B;

        engine.recordTimeout(target);

        assertThat(engine.getWeight(target)).isGreaterThanOrEqualTo(3.5);
        assertThat(engine.getProbability(target, naturalNotes))
                .isGreaterThan(0.30); // Con 7 notas, una sola nota sube a > 30%
    }

    @Test
    @DisplayName("Tardar más tiempo en encontrar una nota debe aumentar su peso frente a notas encontradas rápidamente")
    void testSlowReactionTimeIncreasesWeight() {
        List<Note> naturalNotes = Note.getNaturalNotes();
        Note fastNote = Note.C;
        Note slowNote = Note.G;

        // C se encuentra en 800 ms (muy rápido)
        engine.recordSuccess(fastNote, 800);

        // G se encuentra pero tardando 6000 ms (6 segundos)
        engine.recordSuccess(slowNote, 6000);

        assertThat(engine.getWeight(slowNote)).isGreaterThan(engine.getWeight(fastNote));
        assertThat(engine.getProbability(slowNote, naturalNotes))
                .isGreaterThan(engine.getProbability(fastNote, naturalNotes));
    }

    @Test
    @DisplayName("Aciertos rápidos y consecutivos deben reducir la penalización y restaurar el peso base")
    void testMasteryRecoversBaselineWeight() {
        List<Note> naturalNotes = Note.getNaturalNotes();
        Note target = Note.D;

        // Usuario falla inicialmente
        engine.recordMistake(target);
        engine.recordMistake(target);
        double highWeight = engine.getWeight(target);
        assertThat(highWeight).isGreaterThan(2.5);

        // El usuario practica y ahora acierta varias veces rápidamente (< 1.5s)
        engine.recordSuccess(target, 1100);
        engine.recordSuccess(target, 950);
        engine.recordSuccess(target, 850);

        double recoveredWeight = engine.getWeight(target);
        assertThat(recoveredWeight).isLessThan(highWeight);
        assertThat(recoveredWeight).isCloseTo(1.0, within(0.15));
    }

    @Test
    @DisplayName("selectNextNote no debe repetir inmediatamente la misma nota anterior si hay más de 1 candidata")
    void testSelectNextNoteAvoidsImmediateRepetition() {
        List<Note> naturalNotes = Note.getNaturalNotes();
        Random random = new Random(42);

        Note previous = Note.E;
        for (int i = 0; i < 50; i++) {
            Note next = engine.selectNextNote(naturalNotes, previous, random);
            assertThat(next).isNotEqualTo(previous);
            previous = next;
        }
    }

    @Test
    @DisplayName("Una nota con mayor peso debe ser seleccionada con mayor frecuencia en una muestra estadística")
    void testWeightedSelectionDistribution() {
        List<Note> twoNotes = List.of(Note.C, Note.D);

        // Penalizar fuertemente a D para que tenga ~80% de probabilidad
        engine.recordTimeout(Note.D);
        engine.recordTimeout(Note.D);

        Random random = new Random(12345);
        int dCount = 0;
        int trials = 1000;

        for (int i = 0; i < trials; i++) {
            Note chosen = engine.selectNextNote(twoNotes, null, random);
            if (chosen == Note.D) {
                dCount++;
            }
        }

        // D debe haber salido la gran mayoría de veces debido al peso adaptativo
        double dRatio = (double) dCount / trials;
        assertThat(dRatio).isGreaterThan(0.70);
    }
}

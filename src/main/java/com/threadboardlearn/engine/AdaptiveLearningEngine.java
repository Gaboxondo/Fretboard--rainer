package com.threadboardlearn.engine;

import com.threadboardlearn.model.Note;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Motor de aprendizaje adaptativo y repetición espaciada inteligente.
 * Recalcula dinámicamente las probabilidades de selección de cada nota
 * según los fallos (clics erróneos y tiempo agotado) y el tiempo empleado en encontrarlas.
 */
public class AdaptiveLearningEngine {
    private final Map<Note, NotePerformance> performances = new EnumMap<>(Note.class);
    private final Map<com.threadboardlearn.model.GuitarString, StringPerformance> stringPerformances = new EnumMap<>(com.threadboardlearn.model.GuitarString.class);

    public AdaptiveLearningEngine() {
        for (Note note : Note.values()) {
            performances.put(note, new NotePerformance(note));
        }
        for (com.threadboardlearn.model.GuitarString string : com.threadboardlearn.model.GuitarString.values()) {
            stringPerformances.put(string, new StringPerformance(string));
        }
    }

    public synchronized NotePerformance getPerformance(Note note) {
        return performances.get(note);
    }

    public synchronized Map<Note, NotePerformance> getAllPerformances() {
        return Collections.unmodifiableMap(performances);
    }

    public synchronized StringPerformance getStringPerformance(com.threadboardlearn.model.GuitarString string) {
        return stringPerformances.get(string);
    }

    public synchronized Map<com.threadboardlearn.model.GuitarString, StringPerformance> getAllStringPerformances() {
        return Collections.unmodifiableMap(stringPerformances);
    }

    public synchronized void recordSuccess(Note note, com.threadboardlearn.model.GuitarString string, long reactionTimeMs) {
        if (note != null) {
            performances.get(note).recordSuccess(reactionTimeMs);
        }
        if (string != null) {
            stringPerformances.get(string).recordSuccess(reactionTimeMs);
        }
    }

    public synchronized void recordSuccess(Note note, long reactionTimeMs) {
        recordSuccess(note, null, reactionTimeMs);
    }

    public synchronized void recordMistake(Note note, com.threadboardlearn.model.GuitarString string) {
        if (note != null) {
            performances.get(note).recordMistake();
        }
        if (string != null) {
            stringPerformances.get(string).recordMistake();
        }
    }

    public synchronized void recordMistake(Note note) {
        recordMistake(note, null);
    }

    public synchronized void recordTimeout(Note note, com.threadboardlearn.model.GuitarString string) {
        if (note != null) {
            performances.get(note).recordTimeout();
        }
        if (string != null) {
            stringPerformances.get(string).recordTimeout();
        }
    }

    public synchronized void recordTimeout(Note note) {
        recordTimeout(note, null);
    }

    public synchronized double getWeight(Note note) {
        if (note == null) return 1.0;
        return performances.get(note).getWeight();
    }

    /**
     * Calcula la probabilidad normalizada (0.0 a 1.0) de que salga una nota determinada
     * dentro del grupo de notas activas.
     */
    public synchronized double getProbability(Note note, List<Note> availableNotes) {
        if (availableNotes == null || availableNotes.isEmpty() || note == null) {
            return 0.0;
        }
        double totalWeight = 0.0;
        for (Note n : availableNotes) {
            totalWeight += getWeight(n);
        }
        if (totalWeight <= 0.0) {
            return 1.0 / availableNotes.size();
        }
        return getWeight(note) / totalWeight;
    }

    /**
     * Selecciona probabilísticamente la siguiente nota a entrenar.
     * Evita la repetición inmediata de la misma nota si hay 2 o más notas disponibles.
     */
    public synchronized Note selectNextNote(List<Note> availableNotes, Note previousNote, Random random) {
        if (availableNotes == null || availableNotes.isEmpty()) {
            return null;
        }
        if (availableNotes.size() == 1) {
            return availableNotes.get(0);
        }

        // Filtrar candidatos para no repetir la misma nota dos veces seguidas
        List<Note> candidates;
        if (previousNote != null && availableNotes.contains(previousNote) && availableNotes.size() > 1) {
            candidates = availableNotes.stream().filter(n -> n != previousNote).toList();
        } else {
            candidates = availableNotes;
        }

        if (candidates.isEmpty()) {
            candidates = availableNotes;
        }

        // Muestreo ponderado proporcional al peso acumulado
        double totalCandidateWeight = 0.0;
        for (Note n : candidates) {
            totalCandidateWeight += getWeight(n);
        }

        double randVal = random.nextDouble() * totalCandidateWeight;
        double cumulative = 0.0;
        for (Note candidate : candidates) {
            cumulative += getWeight(candidate);
            if (randVal <= cumulative) {
                return candidate;
            }
        }

        return candidates.get(candidates.size() - 1);
    }

    /**
     * Determina si una nota amerita mostrarse como "nota de refuerzo" en la interfaz.
     */
    public synchronized boolean isFocusNote(Note note, List<Note> availableNotes) {
        if (note == null || availableNotes == null || availableNotes.isEmpty()) {
            return false;
        }
        NotePerformance perf = performances.get(note);
        if (perf == null) return false;

        if (perf.getMistakes() == 0 && perf.getTimeouts() == 0 && perf.getAverageReactionTimeSeconds() <= 2.5) {
            return false;
        }

        double currentWeight = perf.getWeight();
        double avgWeight = availableNotes.stream().mapToDouble(this::getWeight).average().orElse(1.0);

        return currentWeight >= 1.8 && currentWeight > (avgWeight * 1.30);
    }

    /**
     * Devuelve la nota con mayor dificultad acumulada para destacar en la barra de estado.
     */
    public synchronized Note getMostChallengingNote(List<Note> availableNotes) {
        if (availableNotes == null || availableNotes.isEmpty()) {
            return null;
        }
        Note maxNote = null;
        double maxWeight = 1.4; // Umbral mínimo para considerarse reto relevante
        for (Note n : availableNotes) {
            double w = getWeight(n);
            if (w > maxWeight) {
                maxWeight = w;
                maxNote = n;
            }
        }
        return maxNote;
    }

    /**
     * Devuelve la cuerda con mayor número de fallos totales (mistakes + timeouts),
     * o null si no se han registrado fallos en ninguna cuerda.
     */
    public synchronized com.threadboardlearn.model.GuitarString getMostFailedString() {
        com.threadboardlearn.model.GuitarString mostFailed = null;
        int maxFailures = 0;
        for (StringPerformance sp : stringPerformances.values()) {
            int failures = sp.getTotalFailures();
            if (failures > maxFailures) {
                maxFailures = failures;
                mostFailed = sp.getString();
            }
        }
        return mostFailed;
    }

    /**
     * Devuelve las notas ordenadas de mayor a menor número de fallos totales.
     */
    public synchronized List<NotePerformance> getNotesSortedByFailures(List<Note> filterNotes) {
        List<Note> source = (filterNotes != null && !filterNotes.isEmpty())
                ? filterNotes
                : List.of(Note.values());

        return source.stream()
                .map(performances::get)
                .sorted((a, b) -> {
                    int failsA = a.getMistakes() + a.getTimeouts();
                    int failsB = b.getMistakes() + b.getTimeouts();
                    if (failsA != failsB) {
                        return Integer.compare(failsB, failsA); // Descendente
                    }
                    // Si empatan en fallos, ordenar por tiempo medio de reacción
                    return Double.compare(b.getAverageReactionTimeMs(), a.getAverageReactionTimeMs());
                })
                .toList();
    }

    public synchronized void reset() {
        for (NotePerformance perf : performances.values()) {
            perf.reset();
        }
        for (StringPerformance sp : stringPerformances.values()) {
            sp.reset();
        }
    }
}


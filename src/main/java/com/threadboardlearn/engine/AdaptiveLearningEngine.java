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

    public AdaptiveLearningEngine() {
        for (Note note : Note.values()) {
            performances.put(note, new NotePerformance(note));
        }
    }

    public synchronized NotePerformance getPerformance(Note note) {
        return performances.get(note);
    }

    public synchronized Map<Note, NotePerformance> getAllPerformances() {
        return Collections.unmodifiableMap(performances);
    }

    public synchronized void recordSuccess(Note note, long reactionTimeMs) {
        if (note != null) {
            performances.get(note).recordSuccess(reactionTimeMs);
        }
    }

    public synchronized void recordMistake(Note note) {
        if (note != null) {
            performances.get(note).recordMistake();
        }
    }

    public synchronized void recordTimeout(Note note) {
        if (note != null) {
            performances.get(note).recordTimeout();
        }
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

    public synchronized void reset() {
        for (NotePerformance perf : performances.values()) {
            perf.reset();
        }
    }
}

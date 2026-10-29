package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.Memory;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Picks the memories a crew member would think of, following the retrieval
 * score from "Generative Agents" (Park et al., 2023): relevance to the
 * situation + how recent + how important. Each part is min-max normalized
 * across the candidates before adding, because raw cosine similarities sit in
 * a narrow band (about 0.4-0.7) and would otherwise barely change the order.
 */
final class MemoryRanking {

    // A memory's recency halves after about 14 turns
    private static final double RECENCY_DECAY_PER_TURN = 0.95;
    private static final double MAX_IMPORTANCE = 10.0;

    record ScoredMemory(Memory memory, double relevance, double recency, double importance, double total) {
    }

    private MemoryRanking() {
    }

    static List<ScoredMemory> rank(List<Memory> memories, float[] query, int currentTurn, int limit) {
        double[] relevance = memories.stream().mapToDouble(m -> cosineSimilarity(query, m.getEmbedding())).toArray();
        double[] recency = memories.stream()
                .mapToDouble(m -> Math.pow(RECENCY_DECAY_PER_TURN, currentTurn - m.getTurn())).toArray();
        double[] importance = memories.stream()
                .mapToDouble(m -> m.getKind().importance() / MAX_IMPORTANCE).toArray();

        double[] relevanceNorm = normalize(relevance);
        double[] recencyNorm = normalize(recency);
        double[] importanceNorm = normalize(importance);

        return IntStream.range(0, memories.size())
                .mapToObj(i -> new ScoredMemory(memories.get(i), relevance[i], recency[i], importance[i],
                        relevanceNorm[i] + recencyNorm[i] + importanceNorm[i]))
                .sorted(Comparator.comparingDouble(ScoredMemory::total).reversed())
                .limit(limit)
                .toList();
    }

    static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / Math.sqrt(normA * normB);
    }

    // All-equal values carry no ranking signal, so they contribute nothing
    private static double[] normalize(double[] values) {
        double min = Arrays.stream(values).min().orElse(0);
        double max = Arrays.stream(values).max().orElse(0);
        double range = max - min;
        return Arrays.stream(values).map(v -> range == 0 ? 0 : (v - min) / range).toArray();
    }
}

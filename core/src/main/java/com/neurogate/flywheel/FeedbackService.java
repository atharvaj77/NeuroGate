package com.neurogate.flywheel;

import com.neurogate.flywheel.model.GoldenInteraction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FeedbackService - Manages user feedback and golden interactions
 */
@Slf4j
@Service
public class FeedbackService {

    private final Map<String, GoldenInteraction> interactions = new ConcurrentHashMap<>();

    public void saveInteraction(GoldenInteraction interaction) {
        interactions.put(interaction.getId(), interaction);
        log.info("Saved golden interaction: {} (type: {})",
                interaction.getId(), interaction.getFeedbackType());
    }

    public Optional<GoldenInteraction> getInteraction(String id) {
        return Optional.ofNullable(interactions.get(id));
    }

    /**
     * Get all golden interactions (for training)
     */
    public List<GoldenInteraction> getGoldenInteractions() {
        return interactions.values().stream()
                .filter(GoldenInteraction::isGolden)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    public List<GoldenInteraction> getInteractionsByType(GoldenInteraction.FeedbackType type) {
        return interactions.values().stream()
                .filter(i -> type.equals(i.getFeedbackType()))
                .collect(Collectors.toList());
    }

    public List<GoldenInteraction> getInteractionsForModel(String model) {
        return interactions.values().stream()
                .filter(i -> model.equals(i.getModel()))
                .filter(GoldenInteraction::isGolden)
                .collect(Collectors.toList());
    }

    /**
     * Get feedback statistics
     */
    public Map<String, Object> getStatistics() {
        long total = interactions.size();
        long golden = interactions.values().stream().filter(GoldenInteraction::isGolden).count();
        long thumbsUp = interactions.values().stream()
                .filter(i -> GoldenInteraction.FeedbackType.THUMBS_UP.equals(i.getFeedbackType()))
                .count();
        long thumbsDown = interactions.values().stream()
                .filter(i -> GoldenInteraction.FeedbackType.THUMBS_DOWN.equals(i.getFeedbackType()))
                .count();

        return Map.of(
                "total_interactions", total,
                "golden_interactions", golden,
                "thumbs_up", thumbsUp,
                "thumbs_down", thumbsDown,
                "satisfaction_rate", total > 0 ? (double) thumbsUp / total : 0.0);
    }
}

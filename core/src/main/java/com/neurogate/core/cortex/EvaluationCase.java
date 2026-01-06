package com.neurogate.core.cortex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "cortex_cases")
@Data
@NoArgsConstructor
public class EvaluationCase {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String idealOutput;

    // Store context as JSON string in DB, but expose as Map
    @Column(columnDefinition = "TEXT")
    private String contextJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private EvaluationDataset dataset;

    @Transient
    private Map<String, Object> context = new HashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostLoad
    private void loadContext() {
        if (contextJson != null && !contextJson.isEmpty()) {
            try {
                this.context = objectMapper.readValue(contextJson, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {

                this.context = new HashMap<>();
            }
        }
    }

    @PrePersist
    @PreUpdate
    private void saveContext() {
        if (context != null) {
            try {
                this.contextJson = objectMapper.writeValueAsString(context);
            } catch (JsonProcessingException e) {

                this.contextJson = "{}";
            }
        }
    }
}

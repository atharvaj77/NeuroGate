package com.neurogate.sentinel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual choice in the response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Choice {

    private Integer index;

    private Message message;

    private Message delta; // For streaming responses

    @JsonProperty("finish_reason")
    private String finishReason; // "stop", "length", "content_filter"

    private Object logprobs;
}

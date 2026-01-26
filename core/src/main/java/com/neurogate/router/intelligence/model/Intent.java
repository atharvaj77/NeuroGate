package com.neurogate.router.intelligence.model;

/**
 * Intent taxonomy for semantic routing.
 * Each intent maps to optimal models for that task type.
 */
public enum Intent {

    CODE_GENERATION("Generate new code, functions, or programs"),
    CODE_REVIEW("Review, debug, or fix existing code"),
    CODE_EXPLANATION("Explain how code works"),
    REASONING("Complex logical reasoning and analysis"),
    MATH_SCIENCE("Mathematical or scientific problems"),
    CREATIVE_WRITING("Stories, poems, creative content"),
    SUMMARIZATION("Summarize or condense text"),
    TRANSLATION("Translate between languages"),
    QUESTION_ANSWERING("Direct factual questions"),
    CONVERSATION("General conversation and chat"),
    DATA_ANALYSIS("Analyze data, statistics, or metrics"),
    INSTRUCTION_FOLLOWING("Follow specific instructions or tasks");

    private final String description;

    Intent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
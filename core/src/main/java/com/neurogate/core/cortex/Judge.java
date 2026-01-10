package com.neurogate.core.cortex;

public interface Judge {
    /**
     * Grades the output of an agent against the input and ideal output.
     *
     * @param input       The user query or input.
     * @param output      The agent's response.
     * @param idealOutput The expected ideal response (optional).
     * @return A JudgeResult containing score and reasoning.
     */
    JudgeResult grade(String input, String output, String idealOutput);

    /**
     * @return The unique identifier type of this judge (e.g., "faithfulness",
     *         "relevance").
     */
    String getType();
}

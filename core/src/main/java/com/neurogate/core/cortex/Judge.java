package com.neurogate.core.cortex;

public interface Judge {
    /**
     * Grades the output of an agent against the input and ideal output.
     * 
     * @param input       The user query or input.
     * @param output      The agent's response.
     * @param idealOutput The expected ideal response (optional).
     * @return A double score between 0.0 and 1.0.
     */
    double grade(String input, String output, String idealOutput);
}

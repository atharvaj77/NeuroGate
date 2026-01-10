package com.neurogate.synapse.optimizer;

public enum OptimizationObjective {
    FIX_GRAMMAR("Fix Grammar & Tone", "Correct any grammatical errors and ensure a professional, consistent tone."),
    CONCISE("Make Concise",
            "Reduce token usage by rewriting the prompt to be more direct and removing unnecessary words, without losing meaning."),
    REASONING("Enhance Reasoning",
            "Add Chain-of-Thought (CoT) instructions (e.g., 'Let's think step by step') and structural cues to improve complex reasoning capabilities."),
    FEW_SHOT("Generate Few-Shot Examples",
            "Generate 3 high-quality input-output examples relevant to the task to guide the model (Few-Shot Prompting).");

    private final String label;
    private final String instruction;

    OptimizationObjective(String label, String instruction) {
        this.label = label;
        this.instruction = instruction;
    }

    public String getLabel() {
        return label;
    }

    public String getInstruction() {
        return instruction;
    }
}

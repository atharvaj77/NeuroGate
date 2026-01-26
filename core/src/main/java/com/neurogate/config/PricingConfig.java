package com.neurogate.config;

import com.neurogate.config.model.ModelInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for provider pricing and model information.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "neurogate.pricing")
public class PricingConfig {

    // Default costs per 1K tokens (USD)
    // Map key: model name
    // Map value: [input cost, output cost]
    private Map<String, BigDecimal[]> costs = Map.ofEntries(
            // OpenAI Models
            Map.entry("gpt-4o", new BigDecimal[] { new BigDecimal("0.0025"), new BigDecimal("0.01") }),
            Map.entry("gpt-4o-mini", new BigDecimal[] { new BigDecimal("0.00015"), new BigDecimal("0.0006") }),
            Map.entry("gpt-4-turbo", new BigDecimal[] { new BigDecimal("0.01"), new BigDecimal("0.03") }),
            Map.entry("gpt-4", new BigDecimal[] { new BigDecimal("0.03"), new BigDecimal("0.06") }),
            Map.entry("gpt-3.5-turbo", new BigDecimal[] { new BigDecimal("0.0005"), new BigDecimal("0.0015") }),
            Map.entry("o1-preview", new BigDecimal[] { new BigDecimal("0.015"), new BigDecimal("0.06") }),
            Map.entry("o1-mini", new BigDecimal[] { new BigDecimal("0.003"), new BigDecimal("0.012") }),

            // Anthropic Models
            Map.entry("claude-3-5-sonnet-20241022", new BigDecimal[] { new BigDecimal("0.003"), new BigDecimal("0.015") }),
            Map.entry("claude-3-5-haiku-20241022", new BigDecimal[] { new BigDecimal("0.0008"), new BigDecimal("0.004") }),
            Map.entry("claude-3-opus-20240229", new BigDecimal[] { new BigDecimal("0.015"), new BigDecimal("0.075") }),
            Map.entry("claude-3-sonnet-20240229", new BigDecimal[] { new BigDecimal("0.003"), new BigDecimal("0.015") }),
            Map.entry("claude-3-haiku-20240307", new BigDecimal[] { new BigDecimal("0.00025"), new BigDecimal("0.00125") }),

            // Google Models
            Map.entry("gemini-1.5-pro", new BigDecimal[] { new BigDecimal("0.00125"), new BigDecimal("0.005") }),
            Map.entry("gemini-1.5-flash", new BigDecimal[] { new BigDecimal("0.000075"), new BigDecimal("0.0003") }),
            Map.entry("gemini-1.0-pro", new BigDecimal[] { new BigDecimal("0.0005"), new BigDecimal("0.0015") }),

            // AWS Bedrock - Llama
            Map.entry("meta.llama3-1-70b-instruct-v1:0", new BigDecimal[] { new BigDecimal("0.00099"), new BigDecimal("0.00099") }),
            Map.entry("meta.llama3-1-8b-instruct-v1:0", new BigDecimal[] { new BigDecimal("0.00022"), new BigDecimal("0.00022") }),
            Map.entry("meta.llama3-2-90b-instruct-v1:0", new BigDecimal[] { new BigDecimal("0.002"), new BigDecimal("0.002") }),

            // Mistral Models
            Map.entry("mistral-large-latest", new BigDecimal[] { new BigDecimal("0.002"), new BigDecimal("0.006") }),
            Map.entry("mistral-small-latest", new BigDecimal[] { new BigDecimal("0.0002"), new BigDecimal("0.0006") }),
            Map.entry("codestral-latest", new BigDecimal[] { new BigDecimal("0.0002"), new BigDecimal("0.0006") })
    );

    // Model metadata for the /v1/models endpoint
    private static final Map<String, ModelInfo> MODEL_INFO = new HashMap<>();

    static {
        // OpenAI Models
        MODEL_INFO.put("gpt-4o", ModelInfo.builder()
                .id("gpt-4o").name("GPT-4o").provider("openai").family("gpt-4")
                .inputCostPer1k(new BigDecimal("0.0025")).outputCostPer1k(new BigDecimal("0.01"))
                .contextWindow(128000).maxOutputTokens(16384)
                .capabilities(List.of("chat", "function_calling", "vision", "json_mode"))
                .available(true).build());

        MODEL_INFO.put("gpt-4o-mini", ModelInfo.builder()
                .id("gpt-4o-mini").name("GPT-4o Mini").provider("openai").family("gpt-4")
                .inputCostPer1k(new BigDecimal("0.00015")).outputCostPer1k(new BigDecimal("0.0006"))
                .contextWindow(128000).maxOutputTokens(16384)
                .capabilities(List.of("chat", "function_calling", "vision", "json_mode"))
                .available(true).build());

        MODEL_INFO.put("gpt-4-turbo", ModelInfo.builder()
                .id("gpt-4-turbo").name("GPT-4 Turbo").provider("openai").family("gpt-4")
                .inputCostPer1k(new BigDecimal("0.01")).outputCostPer1k(new BigDecimal("0.03"))
                .contextWindow(128000).maxOutputTokens(4096)
                .capabilities(List.of("chat", "function_calling", "vision", "json_mode"))
                .available(true).build());

        MODEL_INFO.put("gpt-3.5-turbo", ModelInfo.builder()
                .id("gpt-3.5-turbo").name("GPT-3.5 Turbo").provider("openai").family("gpt-3.5")
                .inputCostPer1k(new BigDecimal("0.0005")).outputCostPer1k(new BigDecimal("0.0015"))
                .contextWindow(16385).maxOutputTokens(4096)
                .capabilities(List.of("chat", "function_calling", "json_mode"))
                .available(true).build());

        MODEL_INFO.put("o1-preview", ModelInfo.builder()
                .id("o1-preview").name("o1 Preview").provider("openai").family("o1")
                .inputCostPer1k(new BigDecimal("0.015")).outputCostPer1k(new BigDecimal("0.06"))
                .contextWindow(128000).maxOutputTokens(32768)
                .capabilities(List.of("chat", "reasoning"))
                .available(true).build());

        MODEL_INFO.put("o1-mini", ModelInfo.builder()
                .id("o1-mini").name("o1 Mini").provider("openai").family("o1")
                .inputCostPer1k(new BigDecimal("0.003")).outputCostPer1k(new BigDecimal("0.012"))
                .contextWindow(128000).maxOutputTokens(65536)
                .capabilities(List.of("chat", "reasoning"))
                .available(true).build());

        // Anthropic Models
        MODEL_INFO.put("claude-3-5-sonnet-20241022", ModelInfo.builder()
                .id("claude-3-5-sonnet-20241022").name("Claude 3.5 Sonnet").provider("anthropic").family("claude-3.5")
                .inputCostPer1k(new BigDecimal("0.003")).outputCostPer1k(new BigDecimal("0.015"))
                .contextWindow(200000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "function_calling", "vision", "computer_use"))
                .available(true).build());

        MODEL_INFO.put("claude-3-5-haiku-20241022", ModelInfo.builder()
                .id("claude-3-5-haiku-20241022").name("Claude 3.5 Haiku").provider("anthropic").family("claude-3.5")
                .inputCostPer1k(new BigDecimal("0.0008")).outputCostPer1k(new BigDecimal("0.004"))
                .contextWindow(200000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "function_calling"))
                .available(true).build());

        MODEL_INFO.put("claude-3-opus-20240229", ModelInfo.builder()
                .id("claude-3-opus-20240229").name("Claude 3 Opus").provider("anthropic").family("claude-3")
                .inputCostPer1k(new BigDecimal("0.015")).outputCostPer1k(new BigDecimal("0.075"))
                .contextWindow(200000).maxOutputTokens(4096)
                .capabilities(List.of("chat", "function_calling", "vision"))
                .available(true).build());

        // Google Models
        MODEL_INFO.put("gemini-1.5-pro", ModelInfo.builder()
                .id("gemini-1.5-pro").name("Gemini 1.5 Pro").provider("google").family("gemini-1.5")
                .inputCostPer1k(new BigDecimal("0.00125")).outputCostPer1k(new BigDecimal("0.005"))
                .contextWindow(2000000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "function_calling", "vision", "audio", "video"))
                .available(true).build());

        MODEL_INFO.put("gemini-1.5-flash", ModelInfo.builder()
                .id("gemini-1.5-flash").name("Gemini 1.5 Flash").provider("google").family("gemini-1.5")
                .inputCostPer1k(new BigDecimal("0.000075")).outputCostPer1k(new BigDecimal("0.0003"))
                .contextWindow(1000000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "function_calling", "vision", "audio", "video"))
                .available(true).build());

        // AWS Bedrock Llama Models
        MODEL_INFO.put("meta.llama3-1-70b-instruct-v1:0", ModelInfo.builder()
                .id("meta.llama3-1-70b-instruct-v1:0").name("Llama 3.1 70B").provider("bedrock").family("llama-3.1")
                .inputCostPer1k(new BigDecimal("0.00099")).outputCostPer1k(new BigDecimal("0.00099"))
                .contextWindow(128000).maxOutputTokens(4096)
                .capabilities(List.of("chat", "function_calling"))
                .available(true).build());

        MODEL_INFO.put("meta.llama3-1-8b-instruct-v1:0", ModelInfo.builder()
                .id("meta.llama3-1-8b-instruct-v1:0").name("Llama 3.1 8B").provider("bedrock").family("llama-3.1")
                .inputCostPer1k(new BigDecimal("0.00022")).outputCostPer1k(new BigDecimal("0.00022"))
                .contextWindow(128000).maxOutputTokens(4096)
                .capabilities(List.of("chat"))
                .available(true).build());

        // Mistral Models
        MODEL_INFO.put("mistral-large-latest", ModelInfo.builder()
                .id("mistral-large-latest").name("Mistral Large").provider("mistral").family("mistral")
                .inputCostPer1k(new BigDecimal("0.002")).outputCostPer1k(new BigDecimal("0.006"))
                .contextWindow(128000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "function_calling", "json_mode"))
                .available(true).build());

        MODEL_INFO.put("mistral-small-latest", ModelInfo.builder()
                .id("mistral-small-latest").name("Mistral Small").provider("mistral").family("mistral")
                .inputCostPer1k(new BigDecimal("0.0002")).outputCostPer1k(new BigDecimal("0.0006"))
                .contextWindow(32000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "function_calling", "json_mode"))
                .available(true).build());

        MODEL_INFO.put("codestral-latest", ModelInfo.builder()
                .id("codestral-latest").name("Codestral").provider("mistral").family("codestral")
                .inputCostPer1k(new BigDecimal("0.0002")).outputCostPer1k(new BigDecimal("0.0006"))
                .contextWindow(32000).maxOutputTokens(8192)
                .capabilities(List.of("chat", "code"))
                .available(true).build());
    }

    /**
     * Get all available models.
     */
    public List<ModelInfo> getAllModels() {
        return List.copyOf(MODEL_INFO.values());
    }

    /**
     * Get model info by ID.
     */
    public ModelInfo getModelInfo(String modelId) {
        return MODEL_INFO.get(modelId);
    }

    /**
     * Get models by provider.
     */
    public List<ModelInfo> getModelsByProvider(String provider) {
        return MODEL_INFO.values().stream()
                .filter(m -> m.getProvider().equalsIgnoreCase(provider))
                .toList();
    }

    /**
     * Get pricing for a specific model.
     * Returns default low cost if model not found.
     *
     * @param model Model name
     * @return Array containing [input cost, output cost] per 1K tokens
     */
    public BigDecimal[] getCostForModel(String model) {
        return costs.getOrDefault(model, new BigDecimal[] { new BigDecimal("0.001"), new BigDecimal("0.002") });
    }

    /**
     * Calculate estimated cost for a request
     * 
     * @param provider Provider name (unused currently, reserved for future)
     * @param model    Model name
     * @param tokens   Approximate token count
     * @return Estimated cost in USD
     */
    public double getProviderCost(String provider, String model, int tokens) {
        BigDecimal[] rates = getCostForModel(model);
        // Average input/output cost for simplicity in metrics estimates
        BigDecimal averageRate = rates[0].add(rates[1]).divide(new BigDecimal(2), java.math.RoundingMode.HALF_UP);

        return averageRate.multiply(new BigDecimal(tokens))
                .divide(new BigDecimal(1000), java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}

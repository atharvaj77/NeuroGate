package com.neurogate.router.provider;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;

/**
 * Extension interface for providers that support vision/image inputs.
 *
 * <p>Vision providers can process images as part of the conversation,
 * enabling use cases like image analysis, OCR, and visual Q&A.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (provider instanceof VisionProvider vision) {
 *     ChatRequest request = ChatRequest.builder()
 *         .model("gpt-4-vision")
 *         .messages(List.of(Message.withImage(imageUrl, "What's in this image?")))
 *         .build();
 *     ChatResponse response = vision.generateWithVision(request);
 * }
 * }</pre>
 */
public interface VisionProvider extends LLMProvider {

    /**
     * Generate a response for a request containing image inputs.
     *
     * @param request the chat request with image content
     * @return the model's response
     */
    ChatResponse generateWithVision(ChatRequest request);

    /**
     * Check if vision is supported for the given model.
     *
     * @param model the model identifier
     * @return true if the model supports vision inputs
     */
    boolean supportsVision(String model);

    /**
     * Get the maximum image size in bytes.
     */
    default long getMaxImageSize() {
        return 20 * 1024 * 1024; // 20MB default
    }

    /**
     * Get supported image formats.
     */
    default String[] getSupportedImageFormats() {
        return new String[]{"png", "jpeg", "gif", "webp"};
    }
}

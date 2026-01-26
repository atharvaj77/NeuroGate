package com.neurogate.validation;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.validation.model.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for testing JSON schema validation.
 */
@RestController
@RequestMapping("/api/v1/validation")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "JSON schema validation for structured outputs")
public class ValidationController {

    private final StructuredOutputValidator validator;

    @PostMapping("/validate")
    @Operation(
            summary = "Validate JSON against schema",
            description = "Test if a JSON string matches a provided schema"
    )
    public ValidationResult validateJson(@RequestBody ValidateRequest request) {
        return validator.validate(request.content(), request.schema());
    }

    @PostMapping("/extract-json")
    @Operation(
            summary = "Extract JSON from text",
            description = "Extract JSON from text that may contain markdown or other content"
    )
    public ExtractJsonResponse extractJson(@RequestBody ExtractJsonRequest request) {
        String extracted = validator.extractJson(request.content());
        return new ExtractJsonResponse(request.content(), extracted, !request.content().equals(extracted));
    }

    @PostMapping("/auto-fix")
    @Operation(
            summary = "Attempt to auto-fix JSON",
            description = "Try to fix common JSON formatting issues"
    )
    public AutoFixResponse autoFix(@RequestBody AutoFixRequest request) {
        String fixed = validator.attemptAutoFix(request.content());
        return new AutoFixResponse(
                request.content(),
                fixed,
                fixed != null,
                fixed != null ? "Applied fixes" : "No fixes needed"
        );
    }

    public record ValidateRequest(String content, Map<String, Object> schema) {}

    public record ExtractJsonRequest(String content) {}

    public record ExtractJsonResponse(String original, String extracted, boolean modified) {}

    public record AutoFixRequest(String content) {}

    public record AutoFixResponse(String original, String fixed, boolean wasFixed, String message) {}
}
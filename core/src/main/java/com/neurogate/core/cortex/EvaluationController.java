package com.neurogate.core.cortex;

import com.neurogate.core.cortex.dto.AdHocEvaluationRequest;
import com.neurogate.core.cortex.dto.AdHocEvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/cortex")
@RequiredArgsConstructor
@Tag(name = "Cortex", description = "Response evaluation engine for AI quality assessment")
public class EvaluationController {

    private final CortexService cortexService;

    @Operation(summary = "Ad-hoc evaluation", description = "Run evaluation on provided test cases without creating a dataset")
    @ApiResponse(responseCode = "200", description = "Evaluation completed")
    @PostMapping("/evaluate")
    public ResponseEntity<AdHocEvaluationResponse> evaluateAdHoc(@RequestBody AdHocEvaluationRequest request) {
        log.info("Received ad-hoc evaluation request with {} cases", request.getTestCases().size());
        AdHocEvaluationResponse response = cortexService.evaluateAdHoc(request);
        return ResponseEntity.ok(response);
    }
}

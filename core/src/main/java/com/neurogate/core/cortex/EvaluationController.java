package com.neurogate.core.cortex;

import com.neurogate.core.cortex.dto.AdHocEvaluationRequest;
import com.neurogate.core.cortex.dto.AdHocEvaluationResponse;
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
public class EvaluationController {

    private final CortexService cortexService;

    @PostMapping("/evaluate")
    public ResponseEntity<AdHocEvaluationResponse> evaluateAdHoc(@RequestBody AdHocEvaluationRequest request) {
        log.info("Received ad-hoc evaluation request with {} cases", request.getTestCases().size());
        AdHocEvaluationResponse response = cortexService.evaluateAdHoc(request);
        return ResponseEntity.ok(response);
    }
}

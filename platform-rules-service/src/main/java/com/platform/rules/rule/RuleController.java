package com.platform.rules.rule;

import com.platform.common.web.StandardResponseEnvelope;
import com.platform.rules.rule.dto.RuleExecutionRequest;
import com.platform.rules.rule.dto.RuleExecutionResponse;
import com.platform.rules.rule.service.RuleExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleExecutionService ruleExecutionService;

    @PostMapping("/execute")
    public ResponseEntity<StandardResponseEnvelope<RuleExecutionResponse>> execute(
            @Valid @RequestBody RuleExecutionRequest req, HttpServletRequest http) {
        RuleExecutionResponse result = ruleExecutionService.execute(req);
        Object rid = http.getAttribute("requestId");
        return ResponseEntity.ok(StandardResponseEnvelope.of(
                result, rid != null ? rid.toString() : MDC.get("requestId"), MDC.get("traceId")));
    }
}

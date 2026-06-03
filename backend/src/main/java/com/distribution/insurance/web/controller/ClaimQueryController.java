package com.distribution.insurance.web.controller;

import com.distribution.insurance.service.ClaimQueryService;
import com.distribution.insurance.web.dto.BenefitAnalysisResponse;
import com.distribution.insurance.web.dto.ClaimSummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimQueryController {

    private final ClaimQueryService queryService;

    public ClaimQueryController(ClaimQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/status")
    public List<ClaimSummaryResponse> status(@AuthenticationPrincipal Long userId) {
        return queryService.inProgressClaims(userId).stream().map(ClaimSummaryResponse::from).toList();
    }

    @GetMapping("/history")
    public List<ClaimSummaryResponse> history(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queryService.history(userId, from, to).stream().map(ClaimSummaryResponse::from).toList();
    }

    @GetMapping("/benefit-analysis")
    public BenefitAnalysisResponse analysis(@AuthenticationPrincipal Long userId,
                                            @RequestParam Long contractId) {
        return BenefitAnalysisResponse.from(queryService.benefitAnalysis(userId, contractId));
    }
}

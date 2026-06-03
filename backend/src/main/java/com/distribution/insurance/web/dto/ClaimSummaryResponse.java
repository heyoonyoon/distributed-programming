package com.distribution.insurance.web.dto;

import com.distribution.insurance.service.ClaimQueryService;

import java.time.LocalDate;

public record ClaimSummaryResponse(Long claimId, String claimType, LocalDate claimDate,
                                   int requestAmount, long paidAmount, String status) {
    public static ClaimSummaryResponse from(ClaimQueryService.ClaimSummary s) {
        return new ClaimSummaryResponse(s.claimId(), s.claimType(), s.claimDate(),
                s.requestAmount(), s.paidAmount(), s.status());
    }
}

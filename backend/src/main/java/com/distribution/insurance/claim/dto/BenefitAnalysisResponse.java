package com.distribution.insurance.claim.dto;

import com.distribution.insurance.claim.service.ClaimQueryService;

public record BenefitAnalysisResponse(long totalPaidPremium, long totalReceivedBenefit,
                                      long profit, double profitRate) {
    public static BenefitAnalysisResponse from(ClaimQueryService.BenefitAnalysis a) {
        return new BenefitAnalysisResponse(a.totalPaidPremium(), a.totalReceivedBenefit(), a.profit(), a.profitRate());
    }
}

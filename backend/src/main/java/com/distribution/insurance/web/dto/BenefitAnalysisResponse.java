package com.distribution.insurance.web.dto;

import com.distribution.insurance.service.ClaimQueryService;

public record BenefitAnalysisResponse(long totalPaidPremium, long totalReceivedBenefit,
                                      long profit, double profitRate) {
    public static BenefitAnalysisResponse from(ClaimQueryService.BenefitAnalysis a) {
        return new BenefitAnalysisResponse(a.totalPaidPremium(), a.totalReceivedBenefit(), a.profit(), a.profitRate());
    }
}

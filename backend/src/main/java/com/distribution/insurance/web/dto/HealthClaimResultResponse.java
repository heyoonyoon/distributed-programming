package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;

public record HealthClaimResultResponse(Long claimId, String status, String complexity) {
    public static HealthClaimResultResponse from(HealthInsuranceClaim claim) {
        return new HealthClaimResultResponse(
                claim.getId(), claim.getStatus().name(), claim.getComplexity().name());
    }
}

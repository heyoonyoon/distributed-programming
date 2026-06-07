package com.distribution.insurance.claim.domain;

/** Claim 처리 상태(ADR 0007: 다이어그램 4값 + COMPLETED/FAILED). */
public enum ClaimStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED, COMPLETED, FAILED
}

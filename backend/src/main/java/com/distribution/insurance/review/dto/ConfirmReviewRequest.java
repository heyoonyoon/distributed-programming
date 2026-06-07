package com.distribution.insurance.review.dto;

import com.distribution.insurance.review.domain.ReviewResult;
import jakarta.validation.constraints.NotNull;

/** 심사 확정 요청(UC13 7단계). surchargeRate는 조건부승인 시에만. */
public record ConfirmReviewRequest(
        @NotNull ReviewResult result,
        String comment,
        Double surchargeRate) {}

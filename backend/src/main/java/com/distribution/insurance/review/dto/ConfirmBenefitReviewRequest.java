package com.distribution.insurance.review.dto;

import com.distribution.insurance.review.domain.ReviewResult;
import jakarta.validation.constraints.NotNull;

public record ConfirmBenefitReviewRequest(@NotNull ReviewResult result, String comment, Integer payoutAmount) {}

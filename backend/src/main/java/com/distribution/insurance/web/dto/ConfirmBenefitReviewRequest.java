package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.ReviewResult;
import jakarta.validation.constraints.NotNull;

public record ConfirmBenefitReviewRequest(@NotNull ReviewResult result, String comment, Integer payoutAmount) {}

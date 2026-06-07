package com.distribution.insurance.review.controller;

import com.distribution.insurance.review.domain.EnrollmentReview;
import com.distribution.insurance.review.service.ReviewService;
import com.distribution.insurance.review.dto.ConfirmReviewRequest;
import com.distribution.insurance.application.dto.PendingApplicationResponse;
import com.distribution.insurance.review.dto.ReviewDetailResponse;
import com.distribution.insurance.review.dto.ReviewResultResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pending")
    public List<PendingApplicationResponse> pending() {
        return reviewService.pendingApplications().stream()
                .map(PendingApplicationResponse::from)
                .toList();
    }

    @GetMapping("/applications/{id}")
    public ReviewDetailResponse detail(@PathVariable Long id) {
        ReviewService.ReviewDetail detail = reviewService.detail(id);
        return ReviewDetailResponse.from(detail.application(), detail.accidentHistory());
    }

    @PostMapping("/applications/{id}/confirm")
    public ReviewResultResponse confirm(@AuthenticationPrincipal Long reviewerId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody ConfirmReviewRequest request) {
        EnrollmentReview review = reviewService.confirm(
                reviewerId, id, request.result(), request.comment(), request.surchargeRate());
        return ReviewResultResponse.from(review);
    }
}

package com.distribution.insurance.review.controller;

import com.distribution.insurance.review.domain.BenefitPaymentReview;
import com.distribution.insurance.review.repository.BenefitPaymentReviewRepository;
import com.distribution.insurance.review.service.BenefitReviewService;
import com.distribution.insurance.common.service.StaffAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.distribution.insurance.review.dto.BenefitReviewSummaryResponse;
import com.distribution.insurance.review.dto.BenefitReviewDetailResponse;
import com.distribution.insurance.review.dto.ConfirmBenefitReviewRequest;
import com.distribution.insurance.review.dto.BenefitReviewResultResponse;
import com.distribution.insurance.review.dto.AssignStaffRequest;

@RestController
@RequestMapping("/staff")
public class StaffReviewController {

    private final BenefitReviewService reviewService;
    private final StaffAssignmentService assignmentService;
    private final BenefitPaymentReviewRepository reviewRepository;

    public StaffReviewController(BenefitReviewService reviewService,
                                 StaffAssignmentService assignmentService,
                                 BenefitPaymentReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.assignmentService = assignmentService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/benefit-reviews")
    public List<BenefitReviewSummaryResponse> assigned(@AuthenticationPrincipal Long staffId) {
        return reviewService.assignedReviews(staffId).stream()
                .map(BenefitReviewSummaryResponse::from)
                .toList();
    }

    @GetMapping("/benefit-reviews/unassigned")
    public List<BenefitReviewSummaryResponse> unassigned() {
        return reviewService.unassignedReviews().stream()
                .map(BenefitReviewSummaryResponse::from)
                .toList();
    }

    @GetMapping("/benefit-reviews/{claimId}")
    public BenefitReviewDetailResponse detail(@AuthenticationPrincipal Long staffId, @PathVariable Long claimId) {
        return BenefitReviewDetailResponse.from(reviewService.detail(staffId, claimId));
    }

    @PostMapping("/benefit-reviews/{claimId}/confirm")
    public BenefitReviewResultResponse confirm(@AuthenticationPrincipal Long staffId,
                                               @PathVariable Long claimId,
                                               @Valid @RequestBody ConfirmBenefitReviewRequest request) {
        BenefitPaymentReview review = reviewService.confirm(
                staffId, claimId, request.result(), request.comment(), request.payoutAmount());
        return BenefitReviewResultResponse.from(review);
    }

    @PostMapping("/claims/{claimId}/assign")
    public void assign(@PathVariable Long claimId, @Valid @RequestBody AssignStaffRequest request) {
        BenefitPaymentReview review = reviewRepository.findByClaimId(claimId)
                .orElseThrow(() -> new IllegalArgumentException("심사 건을 찾을 수 없습니다."));
        assignmentService.assignManually(review, request.employeeId());
    }

    @PostMapping("/benefit-reviews/{claimId}/retry")
    public void retry(@AuthenticationPrincipal Long staffId, @PathVariable Long claimId) {
        reviewService.retryPayout(staffId, claimId);
    }
}

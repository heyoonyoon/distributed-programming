package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.BenefitPaymentReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 보험금 지급 심사(UC12). 배정 담당자만 확정. 승인 시 지급(UC17 재사용). */
@Service
public class BenefitReviewService {

    private final BenefitPaymentReviewRepository reviewRepository;
    private final BenefitPayoutService payoutService;
    private final NotificationSender notificationSender;

    public BenefitReviewService(BenefitPaymentReviewRepository reviewRepository,
                                BenefitPayoutService payoutService,
                                NotificationSender notificationSender) {
        this.reviewRepository = reviewRepository;
        this.payoutService = payoutService;
        this.notificationSender = notificationSender;
    }

    @Transactional(readOnly = true)
    public List<BenefitPaymentReview> assignedReviews(Long staffId) {
        return reviewRepository.findByAssignedStaffIdWithClaim(staffId).stream()
                .filter(r -> r.getResult() == null)
                .toList();
    }

    @Transactional(readOnly = true)
    public BenefitPaymentReview detail(Long staffId, Long claimId) {
        return requireOwned(staffId, claimId);
    }

    @Transactional
    public BenefitPaymentReview confirm(Long staffId, Long claimId, ReviewResult result, String comment) {
        BenefitPaymentReview review = requireOwned(staffId, claimId);
        if (review.getResult() != null) {
            throw new IllegalStateTransitionException("이미 확정된 심사입니다.");
        }
        Claim claim = review.getClaim();

        review.confirm(result, comment);
        Policyholder ph = claim.getContract().getPolicyholder();

        if (result == ReviewResult.REJECTED) {
            claim.markRejected();
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "보험금 지급 심사 결과: 반려. 사유: " + comment);
        } else {   // APPROVED
            claim.markApproved();
            payoutService.pay(claim);   // 지급 성공 → COMPLETED, 실패 → FAILED + 직원 알림
        }
        return review;
    }

    /** 송금 실패(FAILED) 건 재시도(UC17 E1-3). 배정된 담당자만 재시도 가능. */
    @Transactional
    public void retryPayout(Long staffId, Long claimId) {
        BenefitPaymentReview review = requireOwned(staffId, claimId);
        payoutService.pay(review.getClaim());
    }

    private BenefitPaymentReview requireOwned(Long staffId, Long claimId) {
        BenefitPaymentReview review = reviewRepository.findByClaimIdWithClaim(claimId)
                .orElseThrow(() -> new IllegalArgumentException("심사 건을 찾을 수 없습니다."));
        if (!staffId.equals(review.getAssignedStaffId())) {
            throw new IllegalStateTransitionException("현재 담당자가 처리 중인 건입니다.");
        }
        return review;
    }
}

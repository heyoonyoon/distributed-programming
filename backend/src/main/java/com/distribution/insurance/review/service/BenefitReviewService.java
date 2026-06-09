package com.distribution.insurance.review.service;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.claim.domain.Claim;
import com.distribution.insurance.review.domain.BenefitPaymentReview;
import com.distribution.insurance.review.domain.ReviewResult;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.review.repository.BenefitPaymentReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.distribution.insurance.claim.service.BenefitPayoutService;
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.common.service.IllegalStateTransitionException;
import com.distribution.insurance.common.service.InvalidRequestException;

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

    /** 미배정 보상심사 건(수동 배정 대상). assignedStaffId=null && status=PENDING(접수, 배정 전). */
    @Transactional(readOnly = true)
    public List<BenefitPaymentReview> unassignedReviews() {
        return reviewRepository.findUnassignedWithClaim(
                com.distribution.insurance.claim.domain.ClaimStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public BenefitPaymentReview detail(Long staffId, Long claimId) {
        return requireOwned(staffId, claimId);
    }

    @Transactional
    public BenefitPaymentReview confirm(Long staffId, Long claimId, ReviewResult result, String comment, Integer payoutAmount) {
        BenefitPaymentReview review = requireOwned(staffId, claimId);
        if (review.getResult() != null) {
            throw new IllegalStateTransitionException("이미 확정된 심사입니다.");
        }
        Claim claim = review.getClaim();
        boolean carApprove = result == ReviewResult.APPROVED && claim instanceof CarAccidentReport;
        if (carApprove && (payoutAmount == null || payoutAmount <= 0)) {
            throw new InvalidRequestException("자동차사고 승인 시 지급 보험금(payoutAmount)은 0보다 커야 합니다.");
        }

        review.confirm(result, comment);
        Policyholder ph = claim.getContract().getPolicyholder();

        if (result == ReviewResult.REJECTED) {
            claim.markRejected();
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "보험금 지급 심사 결과: 반려. 사유: " + comment);
        } else {   // APPROVED
            if (claim instanceof CarAccidentReport car) {
                car.assessPayout(payoutAmount);   // 직원이 사정한 금액을 청구금액으로 기록
            }
            claim.markApproved();
            payoutService.pay(claim);   // 지급 성공 → COMPLETED, 실패 → FAILED + 직원 알림
        }
        return review;
    }

    /** 금액이 이미 확정된 건(의료 복잡 청구 등)의 확정. */
    @Transactional
    public BenefitPaymentReview confirm(Long staffId, Long claimId, ReviewResult result, String comment) {
        return confirm(staffId, claimId, result, comment, null);
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

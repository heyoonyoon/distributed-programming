package com.distribution.insurance.repository;

import com.distribution.insurance.domain.review.BenefitPaymentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BenefitPaymentReviewRepository extends JpaRepository<BenefitPaymentReview, Long> {
    List<BenefitPaymentReview> findByAssignedStaffId(Long staffId);
    Optional<BenefitPaymentReview> findByClaimId(Long claimId);
}

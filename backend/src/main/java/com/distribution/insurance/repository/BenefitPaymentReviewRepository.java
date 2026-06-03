package com.distribution.insurance.repository;

import com.distribution.insurance.domain.review.BenefitPaymentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BenefitPaymentReviewRepository extends JpaRepository<BenefitPaymentReview, Long> {
    List<BenefitPaymentReview> findByAssignedStaffId(Long staffId);
    Optional<BenefitPaymentReview> findByClaimId(Long claimId);

    /** claim을 join fetch하여 LAZY 초기화 없이 매핑 가능 (LazyInitializationException 방지). */
    @Query("select r from BenefitPaymentReview r join fetch r.claim where r.assignedStaffId = :staffId")
    List<BenefitPaymentReview> findByAssignedStaffIdWithClaim(@Param("staffId") Long staffId);

    /** claim을 join fetch — requireOwned/detail의 LazyInitializationException 방지. */
    @Query("select r from BenefitPaymentReview r join fetch r.claim where r.claim.id = :claimId")
    Optional<BenefitPaymentReview> findByClaimIdWithClaim(@Param("claimId") Long claimId);
}

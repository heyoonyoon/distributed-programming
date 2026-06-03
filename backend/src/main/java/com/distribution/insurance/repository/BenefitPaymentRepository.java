package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.BenefitPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitPaymentRepository extends JpaRepository<BenefitPayment, Long> {

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(b.paidAmount), 0) from BenefitPayment b where b.claim.contract.id = :contractId and b.status = :status")
    long sumPaidByContractIdAndStatus(Long contractId, com.distribution.insurance.domain.contract.PaymentStatus status);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(b.paidAmount), 0) from BenefitPayment b where b.claim.id = :claimId and b.status = :status")
    long sumPaidByClaimIdAndStatus(Long claimId, com.distribution.insurance.domain.contract.PaymentStatus status);
}

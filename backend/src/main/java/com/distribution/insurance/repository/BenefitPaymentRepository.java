package com.distribution.insurance.repository;

import com.distribution.insurance.domain.claim.BenefitPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitPaymentRepository extends JpaRepository<BenefitPayment, Long> {
}

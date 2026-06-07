package com.distribution.insurance.contract.repository;

import com.distribution.insurance.contract.domain.Payment;
import com.distribution.insurance.contract.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** FIFO 충당용 — 계약의 성공 납부 건수(ADR 0004). */
    long countByContractIdAndStatus(Long contractId, PaymentStatus status);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(p.amount), 0) from Payment p where p.contract.id = :contractId and p.status = :status")
    long sumAmountByContractIdAndStatus(Long contractId, PaymentStatus status);
}

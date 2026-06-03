package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 보험금 지급 1건(06_payment, UC17). SIMPLE 청구는 심사 없이 직접 생성(ADR 0006). */
@Entity
@Table(name = "benefit_payment")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BenefitPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int paidAmount;
    private LocalDateTime paidAt;
    private String bankAccount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    private BenefitPayment(Claim claim, int paidAmount, String bankAccount, PaymentStatus status) {
        this.claim = claim;
        this.paidAmount = paidAmount;
        this.bankAccount = bankAccount;
        this.status = status;
        this.paidAt = LocalDateTime.now();
    }

    public static BenefitPayment success(Claim claim, int paidAmount, String bankAccount) {
        return new BenefitPayment(claim, paidAmount, bankAccount, PaymentStatus.SUCCESS);
    }

    public static BenefitPayment failed(Claim claim, int paidAmount, String bankAccount) {
        return new BenefitPayment(claim, paidAmount, bankAccount, PaymentStatus.FAILED);
    }
}

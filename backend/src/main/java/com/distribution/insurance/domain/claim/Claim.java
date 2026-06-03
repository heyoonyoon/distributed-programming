package com.distribution.insurance.domain.claim;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.service.IllegalStateTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 보상 처리 1건의 추상 부모(04_claim). InsuranceContract와 composition. */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "claim")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate claimDate;
    private int requestAmount;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

    protected Claim(InsuranceContract contract, int requestAmount) {
        this.contract = contract;
        this.requestAmount = requestAmount;
        this.claimDate = LocalDate.now();
        this.status = ClaimStatus.PENDING;
    }

    public void markInReview() {
        requireStatus(ClaimStatus.PENDING);
        this.status = ClaimStatus.IN_REVIEW;
    }

    public void markApproved() {
        requireStatus(ClaimStatus.IN_REVIEW);
        this.status = ClaimStatus.APPROVED;
    }

    public void markRejected() {
        requireStatus(ClaimStatus.IN_REVIEW);
        this.status = ClaimStatus.REJECTED;
    }

    /** 지급 완료. SIMPLE은 PENDING에서, COMPLEX는 APPROVED에서 진입(ADR 0006/0007). */
    public void markCompleted() {
        requireOneOf(ClaimStatus.PENDING, ClaimStatus.APPROVED);
        this.status = ClaimStatus.COMPLETED;
    }

    /** 지급 실패(UC17 E1). 송금 시도는 PENDING(SIMPLE) 또는 APPROVED(COMPLEX)에서만 일어난다. */
    public void markFailed() {
        requireOneOf(ClaimStatus.PENDING, ClaimStatus.APPROVED);
        this.status = ClaimStatus.FAILED;
    }

    private void requireStatus(ClaimStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateTransitionException(
                    "현재 상태(" + status + ")에서 허용되지 않는 전이입니다.");
        }
    }

    private void requireOneOf(ClaimStatus... allowed) {
        for (ClaimStatus s : allowed) {
            if (this.status == s) {
                return;
            }
        }
        throw new IllegalStateTransitionException(
                "현재 상태(" + status + ")에서 허용되지 않는 전이입니다.");
    }
}

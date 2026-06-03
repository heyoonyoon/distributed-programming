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

    public void markCompleted() {
        if (status == ClaimStatus.COMPLETED) {
            throw new IllegalStateTransitionException("이미 지급 완료된 청구입니다.");
        }
        this.status = ClaimStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ClaimStatus.FAILED;
    }

    private void requireStatus(ClaimStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateTransitionException(
                    "현재 상태(" + status + ")에서 허용되지 않는 전이입니다.");
        }
    }
}

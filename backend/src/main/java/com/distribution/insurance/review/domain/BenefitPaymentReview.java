package com.distribution.insurance.review.domain;

import com.distribution.insurance.claim.domain.Claim;
import com.distribution.insurance.common.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 보험금 지급 심사(UC12, ADR 0009). 복잡한 의료보험 청구와 자동차사고 접수 건을 대상으로 하며 배정된 담당자만 확정한다. */
@Entity
@DiscriminatorValue("BENEFIT_PAYMENT")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BenefitPaymentReview extends Review {

    private Long assignedStaffId;   // InsuranceEmployee.id (용어 적응: diagram의 assignedStaffId)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", unique = true)
    private Claim claim;

    public BenefitPaymentReview(Claim claim) {
        this.claim = claim;
    }

    /** 담당자 배정(UC14). */
    public void assignTo(Long staffId) {
        this.assignedStaffId = staffId;
    }

    /** 심사 확정(UC12). 보험금 지급 심사는 승인/반려만 가능(조건부 없음). */
    public void confirm(ReviewResult result, String comment) {
        if (result == ReviewResult.CONDITIONAL) {
            throw new InvalidRequestException("보험금 지급 심사는 조건부 승인을 사용하지 않습니다.");
        }
        recordResult(result, comment);
    }
}

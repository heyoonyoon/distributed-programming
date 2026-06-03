package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 복잡한 의료보험 청구의 보험금 지급 심사(UC12). 배정된 담당자만 확정한다. */
@Entity
@DiscriminatorValue("BENEFIT_PAYMENT")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BenefitPaymentReview extends Review {

    private Long assignedStaffId;   // InsuranceEmployee.id (용어 적응: diagram의 assignedStaffId)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", unique = true)
    private HealthInsuranceClaim claim;

    public BenefitPaymentReview(HealthInsuranceClaim claim) {
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

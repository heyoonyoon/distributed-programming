package com.distribution.insurance.domain.review;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 가입 심사(UC13). 자동차건은 AccidentHistory를 참조한다. */
@Entity
@DiscriminatorValue("ENROLLMENT")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EnrollmentReview extends Review {

    private double surchargeRate;   // 조건부승인 시에만 > 0
    private int adjustedPremium;    // 항상 최종 보험료(ADR 0003)

    @Embedded
    private AccidentHistory accidentHistory;  // 자동차건만, nullable

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true)
    private InsuranceApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private InsuranceEmployee reviewer;

    public EnrollmentReview(InsuranceApplication application, InsuranceEmployee reviewer) {
        this.application = application;
        this.reviewer = reviewer;
    }

    public void attachAccidentHistory(AccidentHistory accidentHistory) {
        this.accidentHistory = accidentHistory;
    }

    /** 심사 확정. adjustedPremium은 결과와 무관하게 최종 보험료를 담는다(ADR 0003). */
    public void confirm(ReviewResult result, String comment, Double surchargeRate, int basePremium) {
        switch (result) {
            case APPROVED -> {
                if (surchargeRate != null) {
                    throw new InvalidRequestException("일반 승인에는 할증율을 입력할 수 없습니다.");
                }
                this.surchargeRate = 0.0;
                this.adjustedPremium = basePremium;
            }
            case CONDITIONAL -> {
                if (surchargeRate == null || surchargeRate <= 0) {
                    throw new InvalidRequestException("조건부 승인은 0보다 큰 할증율이 필요합니다.");
                }
                this.surchargeRate = surchargeRate;
                this.adjustedPremium = applySurcharge(basePremium, surchargeRate);
            }
            case REJECTED -> {
                if (surchargeRate != null) {
                    throw new InvalidRequestException("반려에는 할증율을 입력할 수 없습니다.");
                }
                this.surchargeRate = 0.0;
                this.adjustedPremium = 0;
            }
        }
        recordResult(result, comment);
    }

    private static int applySurcharge(int basePremium, double rate) {
        return (int) Math.round(basePremium * (1 + rate));
    }
}

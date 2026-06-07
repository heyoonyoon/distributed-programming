package com.distribution.insurance.review.dto;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.review.domain.BenefitPaymentReview;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitReviewResponseMappingTest {

    private BenefitPaymentReview carReview() {
        CarAccidentReport report = new CarAccidentReport(null, LocalDate.now(), "서울", "쌍방", "12가3456", true, 2);
        return new BenefitPaymentReview(report);
    }

    @Test
    void 자동차사고_요약은_claimType이_CAR_ACCIDENT이고_hospitalName은_null() {
        var res = BenefitReviewSummaryResponse.from(carReview());
        assertThat(res.claimType()).isEqualTo("CAR_ACCIDENT");
        assertThat(res.hospitalName()).isNull();
        assertThat(res.accidentType()).isEqualTo("쌍방");
    }

    @Test
    void 자동차사고_상세는_사고필드를_담고_의료필드는_null() {
        var res = BenefitReviewDetailResponse.from(carReview());
        assertThat(res.claimType()).isEqualTo("CAR_ACCIDENT");
        assertThat(res.diagnosisCode()).isNull();
        assertThat(res.accidentLocation()).isEqualTo("서울");
        assertThat(res.vehicleNumber()).isEqualTo("12가3456");
        assertThat(res.hasInjury()).isTrue();
    }
}

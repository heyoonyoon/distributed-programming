package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.*;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.ReviewRepository;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ReviewServiceTest {

    @Autowired ReviewService reviewService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ReviewRepository reviewRepository;

    private Policyholder ph() {
        return userRepository.save(new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
    }

    private InsuranceEmployee emp() {
        return userRepository.save(new InsuranceEmployee("심사역", "e@test.com", "010", "pw", "심사팀", 0));
    }

    private InsuranceApplication healthApp(Policyholder ph) {
        InsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        return applicationRepository.save(
                new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음")));
    }

    private InsuranceApplication carApp(Policyholder ph) {
        InsuranceProduct product = productRepository.save(
                new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정"));
        return applicationRepository.save(new InsuranceApplication(
                ph, product, new VehicleInfo("12가3456", "승용차", 2020, 5), null));
    }

    @Test
    void 대기목록은_PENDING만_반환한다() {
        InsuranceApplication app = healthApp(ph());
        assertThat(reviewService.pendingApplications()).extracting(InsuranceApplication::getId)
                .contains(app.getId());
    }

    @Test
    void 자동차건_상세는_사고이력을_동봉한다() {
        InsuranceApplication app = carApp(ph());
        ReviewService.ReviewDetail detail = reviewService.detail(app.getId());
        assertThat(detail.accidentHistory()).isNotNull();
    }

    @Test
    void 의료건_상세는_사고이력이_없다() {
        InsuranceApplication app = healthApp(ph());
        ReviewService.ReviewDetail detail = reviewService.detail(app.getId());
        assertThat(detail.accidentHistory()).isNull();
    }

    @Test
    void 조건부승인_확정시_Application은_APPROVED_보험료는_할증() {
        InsuranceApplication app = healthApp(ph());
        InsuranceEmployee reviewer = emp();

        EnrollmentReview review = reviewService.confirm(
                reviewer.getId(), app.getId(), ReviewResult.CONDITIONAL, "사고이력 다수", 0.2);

        assertThat(review.getAdjustedPremium()).isEqualTo(36000);
        assertThat(applicationRepository.findById(app.getId()).get().getStatus())
                .isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void 이미_심사된_건을_다시_확정하면_409성_예외() {
        InsuranceApplication app = healthApp(ph());
        InsuranceEmployee reviewer = emp();
        reviewService.confirm(reviewer.getId(), app.getId(), ReviewResult.APPROVED, "이상 없음", null);

        assertThatThrownBy(() -> reviewService.confirm(
                reviewer.getId(), app.getId(), ReviewResult.REJECTED, "재심사", null))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}

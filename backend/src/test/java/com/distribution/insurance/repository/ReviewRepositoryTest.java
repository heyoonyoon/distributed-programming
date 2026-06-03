package com.distribution.insurance.repository;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.review.EnrollmentReview;
import com.distribution.insurance.domain.review.ReviewResult;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReviewRepositoryTest {

    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    @Test
    void 심사를_저장하고_신청별로_조회한다() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        em.persist(ph);
        InsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        em.persist(product);
        InsuranceApplication app = new InsuranceApplication(ph, product, null,
                new MedicalHistory("없음", "없음", "없음"));
        em.persist(app);
        InsuranceEmployee emp = new InsuranceEmployee("심사역", "e@test.com", "010", "pw", "심사팀", 0);
        em.persist(emp);

        EnrollmentReview review = new EnrollmentReview(app, emp);
        review.confirm(ReviewResult.APPROVED, "이상 없음", null, 30000);
        reviewRepository.save(review);
        em.flush();
        em.clear();

        assertThat(reviewRepository.findByApplicationId(app.getId())).isPresent();
    }
}

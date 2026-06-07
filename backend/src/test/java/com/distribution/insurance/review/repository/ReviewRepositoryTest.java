package com.distribution.insurance.review.repository;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.product.domain.InsuranceProduct;
import com.distribution.insurance.review.domain.EnrollmentReview;
import com.distribution.insurance.review.domain.ReviewResult;
import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.user.domain.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void 동일_신청에_두_번째_심사_저장시_제약_예외가_발생한다() {
        Policyholder ph = new Policyholder("홍길동", "dup@test.com", "010", "pw",
                "900101-9876543", LocalDate.of(1990, 1, 1), "주소", "계좌");
        em.persist(ph);
        InsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        em.persist(product);
        InsuranceApplication app = new InsuranceApplication(ph, product, null,
                new MedicalHistory("없음", "없음", "없음"));
        em.persist(app);
        InsuranceEmployee emp = new InsuranceEmployee("심사역2", "e2@test.com", "010", "pw", "심사팀", 0);
        em.persist(emp);

        EnrollmentReview first = new EnrollmentReview(app, emp);
        first.confirm(ReviewResult.APPROVED, "이상 없음", null, 30000);
        reviewRepository.saveAndFlush(first);

        EnrollmentReview second = new EnrollmentReview(app, emp);
        second.confirm(ReviewResult.APPROVED, "중복 심사", null, 30000);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(second))
                .isInstanceOfAny(
                        org.springframework.dao.DataIntegrityViolationException.class,
                        jakarta.persistence.PersistenceException.class);
    }
}

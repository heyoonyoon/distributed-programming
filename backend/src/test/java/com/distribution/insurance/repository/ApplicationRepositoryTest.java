package com.distribution.insurance.repository;

import com.distribution.insurance.domain.application.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ApplicationRepositoryTest {

    @Autowired ApplicationRepository applicationRepository;
    @Autowired EntityManager em;

    @Test
    void 신청을_저장하고_신청자별로_조회한다() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        em.persist(ph);
        InsuranceProduct product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        em.persist(product);

        InsuranceApplication app = new InsuranceApplication(
                ph, product, null, new MedicalHistory("없음", "없음", "없음"));
        applicationRepository.save(app);
        em.flush();
        em.clear();

        List<InsuranceApplication> found = applicationRepository.findByApplicantId(ph.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getMedicalHistory().getCurrentConditions()).isEqualTo("없음");
        assertThat(found.get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }
}

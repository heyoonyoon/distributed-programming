package com.distribution.insurance.service;

import com.distribution.insurance.domain.application.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ApplicationRepository;
import com.distribution.insurance.repository.ProductRepository;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ApplicationServiceTest {

    @Autowired ApplicationService applicationService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ApplicationRepository applicationRepository;

    private Long policyholderId() {
        Policyholder ph = userRepository.save(new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        return ph.getId();
    }

    private Long healthProductId() {
        InsuranceProduct p = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        return p.getId();
    }

    @Test
    void 가입신청하면_PENDING으로_저장된다() {
        Long phId = policyholderId();
        Long productId = healthProductId();

        InsuranceApplication app = applicationService.apply(
                phId, productId, null, new MedicalHistory("없음", "없음", "없음"));

        assertThat(app.getId()).isNotNull();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void 없는_상품으로_신청하면_404성_예외() {
        Long phId = policyholderId();
        assertThatThrownBy(() -> applicationService.apply(
                phId, 999999L, null, new MedicalHistory("없음", "없음", "없음")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 본인_PENDING_신청을_취소할_수_있다() {
        Long phId = policyholderId();
        Long productId = healthProductId();
        InsuranceApplication app = applicationService.apply(
                phId, productId, null, new MedicalHistory("없음", "없음", "없음"));

        applicationService.cancel(phId, app.getId());

        assertThat(applicationRepository.findById(app.getId()).get().getStatus())
                .isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void 타인의_신청을_취소하면_403성_예외() {
        Long ownerId = policyholderId();
        Long productId = healthProductId();
        InsuranceApplication app = applicationService.apply(
                ownerId, productId, null, new MedicalHistory("없음", "없음", "없음"));
        Long otherId = userRepository.save(new Policyholder("타인", "x@test.com", "010", "pw",
                "910101-1234567", LocalDate.of(1991, 1, 1), "주소", "계좌")).getId();

        assertThatThrownBy(() -> applicationService.cancel(otherId, app.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}

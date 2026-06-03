package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.ClaimStatus;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CarAccidentServiceTest {

    @Autowired CarAccidentService carAccidentService;
    @Autowired CarAccidentReportRepository reportRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;

    private Policyholder ph(String acct) {
        return userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", acct));
    }

    private InsuranceContract carContract(Policyholder ph) {
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        return contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));
    }

    @Test
    void 접수하면_미배정_review가_생성되고_IN_REVIEW가_된다() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = carContract(p);

        CarAccidentReport report = carAccidentService.report(
                p.getId(), c.getId(), LocalDate.now(), "서울", "쌍방", "12가3456", true, 2, List.of());

        assertThat(report.getId()).isNotNull();
        assertThat(reportRepository.findById(report.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.IN_REVIEW);
        var review = reviewRepository.findByClaimId(report.getId()).orElseThrow();
        assertThat(review.getAssignedStaffId()).isNull();   // 수동 배정 전이므로 미배정
    }

    @Test
    void 본인계약이_아니면_403성_예외() {
        Policyholder owner = ph("110-123-456789");
        Policyholder other = ph("220-123-456789");
        InsuranceContract c = carContract(owner);

        assertThatThrownBy(() -> carAccidentService.report(
                other.getId(), c.getId(), LocalDate.now(), "서울", "단독", "12가3456", false, 0, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 의료보험_계약에_사고접수하면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract health = contractRepository.save(new InsuranceContract(p, product, 30000, LocalDate.now()));

        assertThatThrownBy(() -> carAccidentService.report(
                p.getId(), health.getId(), LocalDate.now(), "서울", "단독", "12가3456", false, 0, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 미래_사고일자는_400성_예외() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = carContract(p);

        assertThatThrownBy(() -> carAccidentService.report(
                p.getId(), c.getId(), LocalDate.now().plusDays(1), "서울", "단독", "12가3456", false, 0, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 필수_사고정보가_공백이면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = carContract(p);

        assertThatThrownBy(() -> carAccidentService.report(
                p.getId(), c.getId(), LocalDate.now(), " ", "단독", "12가3456", false, 0, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 부상없음인데_부상자수가_있으면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = carContract(p);

        assertThatThrownBy(() -> carAccidentService.report(
                p.getId(), c.getId(), LocalDate.now(), "서울", "단독", "12가3456", false, 2, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 대인사고인데_부상자수가_0이면_400성_예외() {
        Policyholder p = ph("110-123-456789");
        InsuranceContract c = carContract(p);

        assertThatThrownBy(() -> carAccidentService.report(
                p.getId(), c.getId(), LocalDate.now(), "서울", "쌍방", "12가3456", true, 0, List.of()))
                .isInstanceOf(InvalidRequestException.class);
    }
}

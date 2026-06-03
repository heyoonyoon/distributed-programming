package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
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
class ClaimQueryServiceTest {

    @Autowired ClaimQueryService queryService;
    @Autowired ClaimRepository claimRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Policyholder ph() {
        return userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
    }

    private InsuranceContract contract(Policyholder ph, LocalDate start) {
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        return contractRepository.save(new InsuranceContract(ph, product, 30000, start));
    }

    @Test
    void 진행중_청구만_현황에_나온다() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now());
        HealthInsuranceClaim pending = claimRepository.save(new HealthInsuranceClaim(
                c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        HealthInsuranceClaim done = new HealthInsuranceClaim(
                c, 300000, "병원", "S00", LocalDate.now(), 300000, ClaimComplexity.SIMPLE);
        done.markCompleted();
        claimRepository.save(done);

        List<ClaimQueryService.ClaimSummary> inProgress = queryService.inProgressClaims(p.getId());
        assertThat(inProgress).extracting(ClaimQueryService.ClaimSummary::claimId)
                .containsExactly(pending.getId());
    }

    @Test
    void 이력은_기간내_청구를_지급액과_함께_반환한다() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now());
        HealthInsuranceClaim claim = claimRepository.save(new HealthInsuranceClaim(
                c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        benefitPaymentRepository.save(BenefitPayment.success(claim, 400000, "110-123-456789"));

        List<ClaimQueryService.ClaimSummary> history = queryService.history(p.getId(),
                LocalDate.now().minusMonths(1), LocalDate.now());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).paidAmount()).isEqualTo(400000L);
    }

    @Test
    void 실익분석은_총납입_총수령_실익을_계산한다() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now().minusMonths(8));
        HealthInsuranceClaim claim = claimRepository.save(new HealthInsuranceClaim(
                c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        paymentRepository.save(Payment.success(c, 30000, PaymentMethod.CARD));
        paymentRepository.save(Payment.success(c, 30000, PaymentMethod.CARD));
        benefitPaymentRepository.save(BenefitPayment.success(claim, 400000, "110-123-456789"));

        ClaimQueryService.BenefitAnalysis a = queryService.benefitAnalysis(p.getId(), c.getId());
        assertThat(a.totalPaidPremium()).isEqualTo(60000L);
        assertThat(a.totalReceivedBenefit()).isEqualTo(400000L);
        assertThat(a.profit()).isEqualTo(340000L);
    }

    @Test
    void 가입_6개월_미만_실익분석은_400성_예외() {
        Policyholder p = ph();
        InsuranceContract c = contract(p, LocalDate.now().minusMonths(2));

        assertThatThrownBy(() -> queryService.benefitAnalysis(p.getId(), c.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 타인_계약_실익분석은_403성_예외() {
        Policyholder owner = ph();
        Policyholder other = ph();
        InsuranceContract c = contract(owner, LocalDate.now().minusMonths(8));

        assertThatThrownBy(() -> queryService.benefitAnalysis(other.getId(), c.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}

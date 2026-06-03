package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.*;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.review.ReviewResult;
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
class BenefitReviewServiceTest {

    @Autowired BenefitReviewService reviewService;
    @Autowired ClaimService claimService;
    @Autowired CarAccidentService carAccidentService;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;

    private Long staffId(String email) {
        return userRepository.save(new InsuranceEmployee("직원", email, "010", "pw", "심사팀", 0)).getId();
    }

    /** 자동차사고를 접수하고 (자동배정된) report를 반환. account로 지급 성공/실패 제어. */
    private CarAccidentReport carReport(String account) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", account));
        var product = productRepository.save(
                new com.distribution.insurance.domain.product.CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        Long contractId = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now())).getId();
        return carAccidentService.report(ph.getId(), contractId, LocalDate.now(), "서울", "쌍방", "12가3456", true, 2, List.of());
    }

    /** COMPLEX 청구를 만들고 (자동배정된) review를 반환. account로 지급 성공/실패 제어. */
    private HealthInsuranceClaim complexClaim(String account) {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", account));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        Long contractId = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now())).getId();
        return claimService.fileHealthClaim(ph.getId(), contractId, "서울병원", "S00",
                LocalDate.now(), 2000000, 2000000, List.of());
    }

    @Test
    void 배정된_담당자가_승인하면_지급되어_COMPLETED된다() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-456789");
        // 자동배정은 유일 직원(staff)에게 감
        reviewService.confirm(staff, claim.getId(), ReviewResult.APPROVED, "정상");

        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 반려하면_REJECTED이고_지급되지_않는다() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-456789");
        reviewService.confirm(staff, claim.getId(), ReviewResult.REJECTED, "서류 미비");

        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void 비배정_담당자가_확정하면_409성_예외() {
        Long assigned = staffId("e@t.com");
        Long other = staffId("o@t.com");   // currentLoad 동일 0 — 자동배정은 id 작은 쪽(assigned)이 먼저
        HealthInsuranceClaim claim = complexClaim("110-123-456789");

        assertThatThrownBy(() -> reviewService.confirm(other, claim.getId(), ReviewResult.APPROVED, "x"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void 승인_후_지급실패건은_재시도로_COMPLETED된다() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-450000");  // 0000 → 송금 실패
        reviewService.confirm(staff, claim.getId(), ReviewResult.APPROVED, "정상");
        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.FAILED);

        // 가입자 계좌가 정상으로 바뀌었다고 가정하고 재시도
        Policyholder ph = claim.getContract().getPolicyholder();
        ph.updateProfile("주소", "110-123-999999");
        reviewService.retryPayout(staff, claim.getId());

        assertThat(claimRepository.findById(claim.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);
    }

    @Test
    void 이미_확정된_심사를_다시_confirm하면_409_예외() {
        Long staff = staffId("e@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-456789");
        reviewService.confirm(staff, claim.getId(), ReviewResult.APPROVED, "정상");

        assertThatThrownBy(() -> reviewService.confirm(staff, claim.getId(), ReviewResult.APPROVED, "중복"))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("이미 확정된 심사입니다.");
    }

    @Test
    void 자동차사고_승인시_직원이_사정한_금액으로_지급되어_COMPLETED된다() {
        Long staff = staffId("e@t.com");
        CarAccidentReport report = carReport("110-123-456789");
        reviewService.confirm(staff, report.getId(), ReviewResult.APPROVED, "정상", 3_000_000);

        var saved = claimRepository.findById(report.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
        assertThat(saved.getRequestAmount()).isEqualTo(3_000_000);
    }

    @Test
    void 자동차사고_승인인데_금액이_없으면_400성_예외() {
        Long staff = staffId("e@t.com");
        CarAccidentReport report = carReport("110-123-456789");
        assertThatThrownBy(() -> reviewService.confirm(staff, report.getId(), ReviewResult.APPROVED, "정상", null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 자동차사고_반려는_금액없이_REJECTED된다() {
        Long staff = staffId("e@t.com");
        CarAccidentReport report = carReport("110-123-456789");
        reviewService.confirm(staff, report.getId(), ReviewResult.REJECTED, "과실 불인정", null);
        assertThat(claimRepository.findById(report.getId()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void 비배정_담당자가_retryPayout_호출하면_409_예외() {
        Long assigned = staffId("e@t.com");
        Long other = staffId("o@t.com");
        HealthInsuranceClaim claim = complexClaim("110-123-450000");  // 0000 → 송금 실패
        reviewService.confirm(assigned, claim.getId(), ReviewResult.APPROVED, "정상");

        assertThatThrownBy(() -> reviewService.retryPayout(other, claim.getId()))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}

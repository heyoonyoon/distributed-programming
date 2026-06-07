package com.distribution.insurance.review.service;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.contract.domain.ContractStatus;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.review.domain.ReviewResult;
import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.user.domain.Policyholder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.application.repository.ApplicationRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;
import com.distribution.insurance.review.repository.ReviewRepository;

@SpringBootTest
class ReviewServiceContractTest {

    @Autowired ReviewService reviewService;
    @Autowired ContractRepository contractRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired ReviewRepository reviewRepository;

    Long employeeId; Long applicationId; Long policyholderId;

    @BeforeEach
    void setUp() {
        contractRepository.deleteAll();
        reviewRepository.deleteAll();
        applicationRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        InsuranceEmployee emp = userRepository.save(
                new InsuranceEmployee("심사원", "emp@test.com", "010-0000-0000", "pw", "심사팀", 0));
        employeeId = emp.getId();
        Policyholder ph = userRepository.save(new Policyholder(
                "홍길동", "ph@test.com", "010-1111-2222", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        policyholderId = ph.getId();
        HealthInsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("실손의료", "설명", 30000, 120));
        InsuranceApplication app = applicationRepository.save(
                new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음")));
        applicationId = app.getId();
    }

    @AfterEach
    void tearDown() {
        contractRepository.deleteAll();
        reviewRepository.deleteAll();
        applicationRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 일반승인_시_계약이_생성되고_월보험료는_상품_기본료() {
        reviewService.confirm(employeeId, applicationId, ReviewResult.APPROVED, "ok", null);

        List<InsuranceContract> contracts = contractRepository.findByPolicyholderId(policyholderId);
        assertThat(contracts).hasSize(1);
        InsuranceContract c = contracts.get(0);
        assertThat(c.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(c.getMonthlyPremium()).isEqualTo(30000);
        assertThat(c.getStartDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void 조건부승인_시_계약_월보험료는_할증_반영된_adjustedPremium() {
        reviewService.confirm(employeeId, applicationId, ReviewResult.CONDITIONAL, "할증", 0.2);

        InsuranceContract c = contractRepository.findByPolicyholderId(policyholderId).get(0);
        assertThat(c.getMonthlyPremium()).isEqualTo(36000); // 30000 * 1.2
    }

    @Test
    void 반려_시_계약은_생성되지_않는다() {
        reviewService.confirm(employeeId, applicationId, ReviewResult.REJECTED, "반려", null);
        assertThat(contractRepository.findByPolicyholderId(policyholderId)).isEmpty();
    }
}

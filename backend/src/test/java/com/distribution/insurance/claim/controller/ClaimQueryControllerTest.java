package com.distribution.insurance.claim.controller;

import com.distribution.insurance.claim.domain.*;
import com.distribution.insurance.contract.domain.*;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.distribution.insurance.claim.repository.ClaimRepository;
import com.distribution.insurance.claim.repository.BenefitPaymentRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimQueryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void tearDown() {
        benefitPaymentRepository.deleteAll();
        claimRepository.deleteAll();
        contractRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 진행중_현황을_조회한다() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "q@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now()));
        claimRepository.save(new HealthInsuranceClaim(c, 500000, "병원", "S00", LocalDate.now(), 500000, ClaimComplexity.SIMPLE));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        mockMvc.perform(get("/claims/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].claimType").value("HEALTH"));
    }

    @Test
    void 실익분석을_조회한다() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "q2@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        InsuranceContract c = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now().minusMonths(8)));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        mockMvc.perform(get("/claims/benefit-analysis").param("contractId", String.valueOf(c.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaidPremium").exists())
                .andExpect(jsonPath("$.profit").exists());
    }
}

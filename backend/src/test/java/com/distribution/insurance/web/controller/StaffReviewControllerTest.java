package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import com.distribution.insurance.security.JwtTokenProvider;
import com.distribution.insurance.repository.BenefitPaymentRepository;
import com.distribution.insurance.service.ClaimService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StaffReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClaimService claimService;
    @Autowired ContractRepository contractRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired ClaimRepository claimRepository;
    @Autowired BenefitPaymentReviewRepository reviewRepository;
    @Autowired BenefitPaymentRepository benefitPaymentRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        benefitPaymentRepository.deleteAll();
        claimRepository.deleteAll();
        contractRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    private InsuranceEmployee emp(String email) {
        return userRepository.save(new InsuranceEmployee("직원", email, "010", "pw", "심사팀", 0));
    }

    private HealthInsuranceClaim complexClaim() {
        Policyholder ph = userRepository.save(new Policyholder("홍", "h" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new HealthInsuranceProduct("건강", "암", 30000, 120));
        Long contractId = contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now())).getId();
        return claimService.fileHealthClaim(ph.getId(), contractId, "서울병원", "S00",
                LocalDate.now(), 2000000, 2000000, List.of());
    }

    @Test
    void 배정담당자가_승인하면_200() throws Exception {
        InsuranceEmployee staff = emp("e@t.com");
        HealthInsuranceClaim claim = complexClaim();   // 유일 직원에게 자동배정
        String token = jwtTokenProvider.createToken(staff.getId(), "EMPLOYEE");

        mockMvc.perform(post("/staff/benefit-reviews/" + claim.getId() + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"APPROVED\",\"comment\":\"정상\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 가입자_토큰으로_직원API_접근하면_403() throws Exception {
        Policyholder ph = userRepository.save(new Policyholder("홍", "p@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        String token = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        mockMvc.perform(get("/staff/benefit-reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 배정담당자의_목록조회는_200이고_claimId_포함() throws Exception {
        InsuranceEmployee staff = emp("e2@t.com");
        HealthInsuranceClaim claim = complexClaim();   // 유일 직원에게 자동배정
        String token = jwtTokenProvider.createToken(staff.getId(), "EMPLOYEE");

        mockMvc.perform(get("/staff/benefit-reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].claimId").value(claim.getId()));
    }
}

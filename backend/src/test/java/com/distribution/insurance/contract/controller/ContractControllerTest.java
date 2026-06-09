package com.distribution.insurance.contract.controller;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;
import com.distribution.insurance.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ContractControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long ownerId; String ownerToken; String otherToken; Long contractId;

    @BeforeEach
    void setUp() {
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        Policyholder owner = userRepository.save(new Policyholder(
                "주인", "owner@test.com", "010-1111-1111", encoder.encode("pw"),
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        ownerId = owner.getId();
        Policyholder other = userRepository.save(new Policyholder(
                "타인", "other@test.com", "010-2222-2222", encoder.encode("pw"),
                "910101-1234567", LocalDate.of(1991, 1, 1), "주소", "계좌"));
        ownerToken = tokenProvider.createToken(ownerId, "POLICYHOLDER");
        otherToken = tokenProvider.createToken(other.getId(), "POLICYHOLDER");

        HealthInsuranceProduct product = productRepository.save(
                new HealthInsuranceProduct("실손의료", "설명", 30000, 120));
        InsuranceContract c = contractRepository.save(
                new InsuranceContract(owner, product, 33000, LocalDate.of(2026, 6, 3)));
        contractId = c.getId();
    }

    @AfterEach
    void tearDown() {
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 내_계약_목록_조회() throws Exception {
        mockMvc.perform(get("/contracts").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monthlyPremium").value(33000))
                .andExpect(jsonPath("$[0].productName").value("실손의료"));
    }

    @Test
    void 계약_상세_조회() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(33000))
                .andExpect(jsonPath("$.paymentMethod").value("미등록"))
                .andExpect(jsonPath("$.coverageItems").isArray());
    }

    @Test
    void 타인_계약_상세는_403() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 없는_계약은_404() throws Exception {
        mockMvc.perform(get("/contracts/999999").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 계약서_PDF_다운로드() throws Exception {
        mockMvc.perform(get("/contracts/" + contractId + "/pdf").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }
}

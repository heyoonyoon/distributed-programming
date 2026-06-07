package com.distribution.insurance.review.controller;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.application.domain.VehicleInfo;
import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.product.domain.InsuranceProduct;
import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.application.repository.ApplicationRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.review.repository.ReviewRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    String empToken; String phToken; Long healthAppId; Long carAppId;

    @AfterEach
    void tearDown() {
        contractRepository.deleteAll();
        reviewRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        contractRepository.deleteAll();
        reviewRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        InsuranceEmployee emp = userRepository.save(new InsuranceEmployee(
                "심사역", "e@test.com", "010", encoder.encode("pw"), "심사팀", 0));
        empToken = tokenProvider.createToken(emp.getId(), "EMPLOYEE");

        Policyholder ph = userRepository.save(new Policyholder("홍길동", "h@test.com", "010", encoder.encode("pw"),
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌"));
        phToken = tokenProvider.createToken(ph.getId(), "POLICYHOLDER");

        InsuranceProduct health = productRepository.save(
                new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120));
        healthAppId = applicationRepository.save(new InsuranceApplication(
                ph, health, null, new MedicalHistory("없음", "없음", "없음"))).getId();

        InsuranceProduct car = productRepository.save(
                new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정"));
        carAppId = applicationRepository.save(new InsuranceApplication(
                ph, car, new VehicleInfo("12가3456", "승용차", 2020, 5), null)).getId();
    }

    @Test
    void 심사대기목록을_조회한다() throws Exception {
        mockMvc.perform(get("/reviews/pending")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 가입자토큰으로_심사목록_접근하면_403() throws Exception {
        mockMvc.perform(get("/reviews/pending")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 자동차건_상세는_사고이력을_포함한다() throws Exception {
        mockMvc.perform(get("/reviews/applications/" + carAppId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleInfo.plateNumber").value("12가3456"))
                .andExpect(jsonPath("$.accidentHistory").exists());
    }

    @Test
    void 의료건_상세는_사고이력이_null() throws Exception {
        mockMvc.perform(get("/reviews/applications/" + healthAppId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicalHistory.currentConditions").value("없음"))
                .andExpect(jsonPath("$.accidentHistory").doesNotExist());
    }

    @Test
    void 조건부승인_확정하면_200과_할증보험료() throws Exception {
        String body = "{\"result\":\"CONDITIONAL\",\"comment\":\"사고이력 다수\",\"surchargeRate\":0.2}";
        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("CONDITIONAL"))
                .andExpect(jsonPath("$.adjustedPremium").value(36000));
    }

    @Test
    void 조건부승인인데_할증율_없으면_400() throws Exception {
        String body = "{\"result\":\"CONDITIONAL\",\"comment\":\"사유\"}";
        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 이미_심사된_건_재확정하면_409() throws Exception {
        String approve = "{\"result\":\"APPROVED\",\"comment\":\"이상 없음\"}";
        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                .header("Authorization", "Bearer " + empToken)
                .contentType("application/json").content(approve))
                .andExpect(status().isOk());

        mockMvc.perform(post("/reviews/applications/" + healthAppId + "/confirm")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType("application/json").content(approve))
                .andExpect(status().isConflict());
    }

    @Test
    void 없는_신청_상세는_404() throws Exception {
        mockMvc.perform(get("/reviews/applications/999999")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isNotFound());
    }
}

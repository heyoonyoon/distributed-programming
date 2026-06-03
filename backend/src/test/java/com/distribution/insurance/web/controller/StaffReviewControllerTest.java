package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import com.distribution.insurance.security.JwtTokenProvider;
import com.distribution.insurance.repository.BenefitPaymentRepository;
import com.distribution.insurance.service.ClaimService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StaffReviewControllerTest {

    @TempDir static Path uploadDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("insurance.upload.dir", () -> uploadDir.toString());
    }

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

    @Test
    void 자동차사고가_보상심사_목록에_뜨고_금액입력으로_승인된다() throws Exception {
        InsuranceEmployee staff = emp("car-staff@t.com");
        Policyholder ph = userRepository.save(new Policyholder("홍", "car-ph" + System.nanoTime() + "@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789"));
        var product = productRepository.save(new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL"));
        InsuranceContract contract = contractRepository.save(new InsuranceContract(ph, product, 50000, LocalDate.now()));
        String phToken = jwtTokenProvider.createToken(ph.getId(), "POLICYHOLDER");
        String staffToken = jwtTokenProvider.createToken(staff.getId(), "EMPLOYEE");

        MockMultipartFile photo = new MockMultipartFile("attachments", "scene.jpg", "image/jpeg", new byte[]{1, 2});

        // 자동차 사고 접수 → claimId 추출
        String responseBody = mockMvc.perform(multipart("/claims/car-accidents")
                        .file(photo)
                        .param("contractId", String.valueOf(contract.getId()))
                        .param("accidentDate", LocalDate.now().toString())
                        .param("accidentLocation", "서울 강남")
                        .param("accidentType", "쌍방")
                        .param("vehicleNumber", "12가3456")
                        .param("hasInjury", "true")
                        .param("injuredCount", "2")
                        .header("Authorization", "Bearer " + phToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // reportId 파싱 (응답: {"reportId":N, "status":"IN_REVIEW"})
        long claimId = Long.parseLong(responseBody.replaceAll(".*\"reportId\":(\\d+).*", "$1"));

        // 자동차사고는 미배정으로 생성되므로 관리자가 담당자를 수동 배정한다(UC14 A1)
        mockMvc.perform(post("/staff/claims/" + claimId + "/assign")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + staff.getId() + "}"))
                .andExpect(status().isOk());

        // 보상심사 목록에 CAR_ACCIDENT 항목이 있어야 한다
        mockMvc.perform(get("/staff/benefit-reviews")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.claimId == " + claimId + ")].claimType",
                        hasItem("CAR_ACCIDENT")));

        // payoutAmount 포함 승인
        mockMvc.perform(post("/staff/benefit-reviews/" + claimId + "/confirm")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"APPROVED\",\"comment\":\"정상\",\"payoutAmount\":3000000}"))
                .andExpect(status().isOk());
    }
}

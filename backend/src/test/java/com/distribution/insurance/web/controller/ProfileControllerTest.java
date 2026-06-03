package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokenProvider;

    Long userId;
    String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Policyholder ph = userRepository.save(new Policyholder(
                "홍길동", "old@test.com", "010-old", encoder.encode("1234"),
                "ssn", LocalDate.of(1990, 1, 1), "옛주소", "옛계좌"));
        userId = ph.getId();
        token = tokenProvider.createToken(userId, "POLICYHOLDER");
    }

    @Test
    void 토큰없이_접근하면_401() throws Exception {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 개인정보를_수정하면_변경값이_반영된다() throws Exception {
        mockMvc.perform(put("/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"email\":\"new@test.com\",\"phone\":\"010-new\","
                                + "\"address\":\"새주소\",\"bankAccount\":\"새계좌\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.address").value("새주소"));
    }
}

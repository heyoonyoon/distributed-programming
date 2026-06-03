package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.domain.user.User;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    UserRepository userRepository = mock(UserRepository.class);
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!", 3600000L);
    AuthService authService = new AuthService(userRepository, encoder, tokenProvider);

    private User policyholder() {
        return new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc");
    }

    @Test
    void 올바른_비번이면_검증가능한_토큰을_발급한다() {
        when(userRepository.findByEmail("h@test.com")).thenReturn(Optional.of(policyholder()));

        String token = authService.login("h@test.com", "1234");

        assertThat(tokenProvider.validate(token)).isTrue();
    }

    @Test
    void 틀린_비번이면_예외() {
        when(userRepository.findByEmail("h@test.com")).thenReturn(Optional.of(policyholder()));
        assertThatThrownBy(() -> authService.login("h@test.com", "9999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 없는_이메일이면_예외() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login("none@test.com", "1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

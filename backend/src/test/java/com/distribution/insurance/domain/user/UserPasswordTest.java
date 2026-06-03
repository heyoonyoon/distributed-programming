package com.distribution.insurance.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserPasswordTest {

    PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void checkPassword_올바른_비번이면_true() {
        Policyholder user = new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc");
        assertThat(user.checkPassword("1234", encoder)).isTrue();
    }

    @Test
    void checkPassword_틀린_비번이면_false() {
        Policyholder user = new Policyholder("홍길동", "h@test.com", "010", encoder.encode("1234"),
                "ssn", LocalDate.now(), "addr", "acc");
        assertThat(user.checkPassword("9999", encoder)).isFalse();
    }
}

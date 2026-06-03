package com.distribution.insurance.repository;

import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void findByEmail_저장된_Policyholder를_찾는다() {
        Policyholder saved = userRepository.save(new Policyholder(
                "홍길동", "hong@test.com", "010-1111-2222", "hashed-pw",
                "901103-1234567", LocalDate.of(1990, 11, 3), "서울시", "111-222-333"));

        Optional<com.distribution.insurance.domain.user.User> found =
                userRepository.findByEmail("hong@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get()).isInstanceOf(Policyholder.class);
    }

    @Test
    void findByEmail_없는_이메일이면_빈값() {
        assertThat(userRepository.findByEmail("none@test.com")).isEmpty();
    }
}

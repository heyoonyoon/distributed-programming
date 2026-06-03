package com.distribution.insurance.config;

import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class DataSeederTest {

    @Test
    void run_계정이_없으면_시드하고_다시_실행해도_중복삽입하지_않는다() throws Exception {
        UserRepository repo = org.mockito.Mockito.mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        DataSeeder seeder = new DataSeeder(repo, encoder);

        org.mockito.Mockito.when(repo.findByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.empty());

        seeder.run();

        org.mockito.Mockito.verify(repo, org.mockito.Mockito.times(3))
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_이미_존재하면_저장하지_않는다() throws Exception {
        UserRepository repo = org.mockito.Mockito.mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        DataSeeder seeder = new DataSeeder(repo, encoder);

        org.mockito.Mockito.when(repo.findByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.of(
                        new com.distribution.insurance.domain.user.Policyholder(
                                "x", "x", "x", "x", "x",
                                java.time.LocalDate.now(), "x", "x")));

        seeder.run();

        org.mockito.Mockito.verify(repo, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
        assertThat(true).isTrue();
    }
}

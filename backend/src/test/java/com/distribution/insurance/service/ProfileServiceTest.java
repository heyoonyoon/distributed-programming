package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    UserRepository userRepository = mock(UserRepository.class);
    IdentityVerificationService identityVerification = mock(IdentityVerificationService.class);
    ProfileService profileService = new ProfileService(userRepository, identityVerification);

    private Policyholder policyholder() {
        return new Policyholder("홍길동", "old@test.com", "010-old", "pw",
                "ssn", LocalDate.of(1990, 1, 1), "옛주소", "옛계좌");
    }

    @Test
    void 본인인증_통과시_연락처와_가입자정보가_모두_갱신된다() {
        Policyholder ph = policyholder();
        when(identityVerification.verify(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        profileService.updateProfile(1L, "new@test.com", "010-new", "새주소", "새계좌");

        assertThat(ph.getEmail()).isEqualTo("new@test.com");
        assertThat(ph.getPhone()).isEqualTo("010-new");
        assertThat(ph.getAddress()).isEqualTo("새주소");
        assertThat(ph.getBankAccount()).isEqualTo("새계좌");
    }
}

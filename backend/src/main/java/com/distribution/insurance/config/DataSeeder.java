package com.distribution.insurance.config;

import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seedPolicyholder("hong@test.com", "홍길동", "010-1111-1111",
                "900101-1234567", LocalDate.of(1990, 1, 1), "서울시 강남구", "110-111-111111");
        seedPolicyholder("kim@test.com", "김보험", "010-2222-2222",
                "850505-2345678", LocalDate.of(1985, 5, 5), "부산시 해운대구", "220-222-222222");
        seedEmployee("staff@test.com", "이심사", "010-9999-9999", "심사팀");
    }

    private void seedPolicyholder(String email, String name, String phone,
                                  String ssn, LocalDate birthDate, String address, String bankAccount) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(new Policyholder(
                name, email, phone, encoder.encode("1234"), ssn, birthDate, address, bankAccount));
    }

    private void seedEmployee(String email, String name, String phone, String department) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(new InsuranceEmployee(
                name, email, phone, encoder.encode("1234"), department, 0));
    }
}

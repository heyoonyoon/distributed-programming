package com.distribution.insurance.config;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.CoverageItem;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    public DataSeeder(UserRepository userRepository, PasswordEncoder encoder,
                      ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        seedPolicyholder("hong@test.com", "홍길동", "010-1111-1111",
                "900101-1234567", LocalDate.of(1990, 1, 1), "서울시 강남구", "110-111-111111");
        seedPolicyholder("kim@test.com", "김보험", "010-2222-2222",
                "850505-2345678", LocalDate.of(1985, 5, 5), "부산시 해운대구", "220-222-222222");
        seedEmployee("staff@test.com", "이심사", "010-9999-9999", "심사팀");
        seedProducts();
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

    private void seedProducts() {
        if (productRepository.count() > 0) return;

        HealthInsuranceProduct healthPlus = new HealthInsuranceProduct(
                "건강플러스", "암·주요질환 중심 의료보험. 기본 보험료는 30대 기준 예시이며 나이·병력에 따라 산출됩니다.",
                30000, 120);
        healthPlus.addCoverageItem(new CoverageItem("암진단비", 30_000_000, 0));
        healthPlus.addCoverageItem(new CoverageItem("암수술비", 10_000_000, 0));
        healthPlus.addCoverageItem(new CoverageItem("입원비", 5_000_000, 100_000));
        productRepository.save(healthPlus);

        HealthInsuranceProduct silson = new HealthInsuranceProduct(
                "실손기본", "통원·입원 실손 보장. 기본 보험료는 30대 기준 예시입니다.",
                12000, 60);
        silson.addCoverageItem(new CoverageItem("통원치료비", 5_000_000, 10_000));
        silson.addCoverageItem(new CoverageItem("입원치료비", 30_000_000, 200_000));
        productRepository.save(silson);

        CarInsuranceProduct safeDrive = new CarInsuranceProduct(
                "안심드라이브", "대인·대물 종합 자동차보험. 차종·운전범위·사고이력에 따라 보험료가 산출됩니다.",
                45000, "승용차", "가족한정");
        safeDrive.addCoverageItem(new CoverageItem("대인배상", 100_000_000, 0));
        safeDrive.addCoverageItem(new CoverageItem("대물배상", 50_000_000, 200_000));
        productRepository.save(safeDrive);

        CarInsuranceProduct lightCar = new CarInsuranceProduct(
                "가벼운자차", "자기차량손해 중심 보급형 자동차보험. 운전범위 누구나.",
                28000, "경차", "누구나");
        lightCar.addCoverageItem(new CoverageItem("자기차량손해", 20_000_000, 300_000));
        productRepository.save(lightCar);
    }
}

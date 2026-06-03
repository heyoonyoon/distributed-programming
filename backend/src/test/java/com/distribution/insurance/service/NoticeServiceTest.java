package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.contract.Notice;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class NoticeServiceTest {

    @Autowired NoticeService noticeService;
    @Autowired ContractRepository contractRepository;
    @Autowired NoticeRepository noticeRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    Long contractId; Long policyholderId;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        Policyholder ph = userRepository.save(TestEntities.policyholder());
        policyholderId = ph.getId();
        HealthInsuranceProduct product = productRepository.save(TestEntities.healthProduct());
        // 4개월 전 시작 → 미납 누적·연체
        InsuranceContract c = contractRepository.save(
                new InsuranceContract(ph, product, 30000, LocalDate.now().minusMonths(4)));
        contractId = c.getId();
    }

    @AfterEach
    void tearDown() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 연체_계약에_고지서가_생성되고_발송된다() {
        int created = noticeService.issueOverdueNotices(LocalDate.now());
        assertThat(created).isEqualTo(1);
        List<Notice> notices = noticeRepository.findByContractId(contractId);
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).isDelivered()).isTrue();
        assertThat(notices.get(0).getAttempts()).isEqualTo(1);
    }

    @Test
    void 같은_날_재실행하면_중복_고지하지_않는다() {
        noticeService.issueOverdueNotices(LocalDate.now());
        int secondRun = noticeService.issueOverdueNotices(LocalDate.now());
        assertThat(secondRun).isZero();
        assertThat(noticeRepository.findByContractId(contractId)).hasSize(1);
    }

    @Test
    void 연체가_없으면_고지서가_생성되지_않는다() {
        // 오늘 시작 계약(연체 없음)만 남기고 기존 계약 제거
        contractRepository.deleteAll();
        Policyholder ph = (Policyholder) userRepository.findById(policyholderId).orElseThrow();
        contractRepository.save(
                new InsuranceContract(ph, productRepository.findAll().get(0), 30000, LocalDate.now()));
        int created = noticeService.issueOverdueNotices(LocalDate.now());
        assertThat(created).isZero();
    }

    @Test
    void 연체_30일_초과면_해지예고_고지서가_생성된다() {
        // setUp의 4개월 전 시작 계약은 첫 회차(약 120일) 연체 → 30일 초과
        noticeService.issueOverdueNotices(LocalDate.now());
        Notice n = noticeRepository.findByContractId(contractId).get(0);
        assertThat(n.isTerminationWarning()).isTrue();
    }
}

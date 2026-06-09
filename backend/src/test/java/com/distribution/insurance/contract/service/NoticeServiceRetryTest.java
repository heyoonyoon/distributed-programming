package com.distribution.insurance.contract.service;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.domain.Notice;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.contract.repository.NoticeRepository;
import com.distribution.insurance.product.repository.ProductRepository;
import com.distribution.insurance.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.common.service.TestEntities;

/** UC16 E1: 발송 실패 시 최대 3회 재시도 후 미발송으로 기록한다. */
@SpringBootTest
class NoticeServiceRetryTest {

    @Autowired NoticeService noticeService;
    @Autowired ContractRepository contractRepository;
    @Autowired NoticeRepository noticeRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    @MockitoBean NotificationSender notificationSender;

    Long contractId;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        Policyholder ph = userRepository.save(TestEntities.policyholder());
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
    void 발송이_계속_실패하면_3회_시도_후_미발송으로_기록된다() {
        Mockito.doThrow(new RuntimeException("발송 실패"))
                .when(notificationSender).send(ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

        noticeService.issueOverdueNotices(LocalDate.now());

        Notice n = noticeRepository.findByContractId(contractId).get(0);
        assertThat(n.isDelivered()).isFalse();
        assertThat(n.getAttempts()).isEqualTo(3);
    }

    @Test
    void 첫_시도_실패_후_재시도에서_성공하면_발송됨으로_2회_기록된다() {
        Mockito.doThrow(new RuntimeException("일시 실패"))
                .doNothing()
                .when(notificationSender).send(ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

        noticeService.issueOverdueNotices(LocalDate.now());

        Notice n = noticeRepository.findByContractId(contractId).get(0);
        assertThat(n.isDelivered()).isTrue();
        assertThat(n.getAttempts()).isEqualTo(2);
    }
}

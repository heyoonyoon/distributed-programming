package com.distribution.insurance.contract.service;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.InsuranceEmployee;
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
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.common.service.TestEntities;

/** UC16 A1: 연체 30일 초과(해지예고) 시 직원에게도 별도 알림이 발송된다. */
@SpringBootTest
class NoticeServiceEmployeeAlertTest {

    @Autowired NoticeService noticeService;
    @Autowired ContractRepository contractRepository;
    @Autowired NoticeRepository noticeRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    @MockitoBean NotificationSender notificationSender;

    String employeeEmail;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        Policyholder ph = userRepository.save(TestEntities.policyholder());
        InsuranceEmployee emp = userRepository.save(TestEntities.employee());
        employeeEmail = emp.getEmail();
        HealthInsuranceProduct product = productRepository.save(TestEntities.healthProduct());
        // 4개월 전 시작 → 연체 30일 초과(해지예고)
        contractRepository.save(new InsuranceContract(ph, product, 30000, LocalDate.now().minusMonths(4)));
    }

    @AfterEach
    void tearDown() {
        noticeRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 해지예고_연체시_직원에게_알림이_발송된다() {
        noticeService.issueOverdueNotices(LocalDate.now());

        Mockito.verify(notificationSender).send(
                ArgumentMatchers.eq(employeeEmail),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.contains("해지예고"));
    }
}

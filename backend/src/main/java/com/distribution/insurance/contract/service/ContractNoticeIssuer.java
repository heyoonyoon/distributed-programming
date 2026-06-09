package com.distribution.insurance.contract.service;

import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.contract.repository.NoticeRepository;
import com.distribution.insurance.contract.repository.PaymentRepository;
import com.distribution.insurance.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.contract.domain.Notice;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.domain.BillingStatus;
import com.distribution.insurance.contract.domain.ContractStatus;
import com.distribution.insurance.contract.domain.PaymentStatus;
import com.distribution.insurance.contract.domain.BillingCalculator;
import com.distribution.insurance.product.dto.ProductTypeMapper;

/**
 * 단일 계약의 미납 고지서 발행(UC16). NoticeService가 계약마다 호출한다.
 * 계약 하나당 독립 트랜잭션으로 처리하여, 한 계약의 실패가 다른 계약의 발행을 롤백하지 않도록 격리한다.
 * (Spring 프록시 트랜잭션을 위해 NoticeService와 별도 빈으로 분리한다.)
 */
@Component
public class ContractNoticeIssuer {

    private static final Logger log = LoggerFactory.getLogger(ContractNoticeIssuer.class);
    private static final int MAX_SEND_ATTEMPTS = 3;   // UC16 E1: 최대 3회 재시도

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public ContractNoticeIssuer(ContractRepository contractRepository,
                                PaymentRepository paymentRepository,
                                NoticeRepository noticeRepository,
                                UserRepository userRepository,
                                NotificationSender notificationSender) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * 계약 하나가 연체 상태면 고지서를 생성·발송·기록한다. 생성했으면 true, 건너뛰었으면 false.
     * 같은 날 이미 고지한 계약은 건너뛴다(사전 체크 + DB 유니크 제약 이중 방어).
     */
    @Transactional
    public boolean issueIfOverdue(Long contractId, LocalDate asOf) {
        InsuranceContract c = contractRepository.findById(contractId).orElse(null);
        if (c == null) return false;
        if (c.getStatus() != ContractStatus.ACTIVE) return false;
        if (noticeRepository.existsByContractIdAndIssuedAt(c.getId(), asOf)) return false;

        long success = paymentRepository.countByContractIdAndStatus(c.getId(), PaymentStatus.SUCCESS);
        BillingStatus status = BillingCalculator.compute(c, success, asOf);
        if (!status.isOverdue()) return false;

        Notice notice = Notice.of(c, status, asOf);
        dispatch(notice, c);
        try {
            noticeRepository.saveAndFlush(notice);
        } catch (DataIntegrityViolationException e) {
            // 동시 실행 등으로 같은 날 중복 발행이 시도된 경우: 유니크 제약(uk_notice_contract_day)으로 차단됨.
            log.warn("미납 고지서 중복 발행 차단(유니크 제약): contractId={}", c.getId());
            return false;
        }
        return true;
    }

    /** 가입자에게 발송(최대 3회 재시도). 30일 초과 시 직원에게도 알린다. */
    private void dispatch(Notice notice, InsuranceContract contract) {
        String message = notice.buildMessage(contract.getProduct().getProductName());
        String email = contract.getPolicyholder().getEmail();
        String phone = contract.getPolicyholder().getPhone();

        boolean delivered = false;
        int attempt = 0;
        while (attempt < MAX_SEND_ATTEMPTS && !delivered) {
            attempt++;
            try {
                notificationSender.send(email, phone, message);
                delivered = true;
            } catch (RuntimeException e) {
                log.warn("미납 고지서 발송 실패(시도 {}/{}): contractId={}", attempt, MAX_SEND_ATTEMPTS, contract.getId());
            }
        }
        notice.markSent(delivered, attempt);

        if (!delivered) {
            // E1: 최종 실패 → 관리자 알림 + 기록
            log.error("미납 고지서 발송 최종 실패: contractId={} (관리자 확인 필요)", contract.getId());
        }
        if (notice.isTerminationWarning()) {
            // A1: 30일 초과 → 직원 별도 알림
            for (InsuranceEmployee emp : userRepository.findAllEmployees()) {
                // best-effort: 개별 직원 알림 실패가 당일 배치 트랜잭션을 롤백시키지 않도록 격리한다.
                try {
                    notificationSender.send(emp.getEmail(), emp.getPhone(),
                            "[해지예고] 계약 " + contract.getId() + " 연체 30일 초과. 확인 바랍니다.");
                } catch (RuntimeException e) {
                    log.warn("직원 해지예고 알림 발송 실패: contractId={}, employeeId={}", contract.getId(), emp.getId());
                }
            }
        }
    }
}

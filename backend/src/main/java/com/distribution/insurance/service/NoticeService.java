package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.NoticeRepository;
import com.distribution.insurance.repository.PaymentRepository;
import com.distribution.insurance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 미납 고지서 발송(UC16). 스케줄러가 issueOverdueNotices를 호출한다. */
@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);
    private static final int MAX_SEND_ATTEMPTS = 3;   // UC16 E1: 최대 3회 재시도

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public NoticeService(ContractRepository contractRepository,
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
     * 연체 계약을 찾아 고지서를 생성·발송·기록한다. 생성된 고지서 수를 반환한다.
     * 같은 날 이미 고지한 계약은 건너뛴다(중복 방지).
     */
    @Transactional
    public int issueOverdueNotices(LocalDate asOf) {
        int created = 0;
        for (InsuranceContract c : contractRepository.findAll()) {
            if (c.getStatus() != ContractStatus.ACTIVE) continue;
            if (noticeRepository.existsByContractIdAndIssuedAt(c.getId(), asOf)) continue;

            long success = paymentRepository.countByContractIdAndStatus(c.getId(), PaymentStatus.SUCCESS);
            BillingStatus status = BillingCalculator.compute(c, success, asOf);
            if (!status.isOverdue()) continue;

            Notice notice = Notice.of(c, status, asOf);
            dispatch(notice, c);
            noticeRepository.save(notice);
            created++;
        }
        return created;
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

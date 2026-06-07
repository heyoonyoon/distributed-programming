package com.distribution.insurance.contract.service;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.repository.ContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 미납 고지서 발송 배치(UC16). 스케줄러가 issueOverdueNotices를 호출한다.
 * 계약별 실제 발행은 ContractNoticeIssuer에 위임하여 계약 단위로 트랜잭션을 격리한다
 * (한 계약 실패가 이미 발행된 다른 계약을 롤백하거나 배치를 중단시키지 않는다).
 */
@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final ContractRepository contractRepository;
    private final ContractNoticeIssuer contractNoticeIssuer;

    public NoticeService(ContractRepository contractRepository,
                         ContractNoticeIssuer contractNoticeIssuer) {
        this.contractRepository = contractRepository;
        this.contractNoticeIssuer = contractNoticeIssuer;
    }

    /**
     * 연체 계약을 찾아 고지서를 생성·발송·기록한다. 생성된 고지서 수를 반환한다.
     * 계약마다 독립 트랜잭션으로 처리하며, 한 계약의 예외는 기록 후 건너뛴다.
     */
    public int issueOverdueNotices(LocalDate asOf) {
        int created = 0;
        for (InsuranceContract c : contractRepository.findAll()) {
            Long contractId = c.getId();
            try {
                if (contractNoticeIssuer.issueIfOverdue(contractId, asOf)) {
                    created++;
                }
            } catch (RuntimeException e) {
                // 한 계약의 처리 실패가 나머지 배치를 중단시키지 않도록 격리한다.
                log.error("미납 고지서 발행 실패(건너뜀): contractId={}", contractId, e);
            }
        }
        return created;
    }
}

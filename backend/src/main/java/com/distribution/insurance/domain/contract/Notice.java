package com.distribution.insurance.domain.contract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 미납 고지서(UC16). 스케줄러가 자동 생성한다.
 * 다이어그램(issuedAt/dueAmount/overdueDays/isTerminationWarning)에 발송 기록 필드를 보강했다(plan 참고).
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Notice {

    /** 연체 30일 초과 시 해지예고(UC16 A1). */
    private static final int TERMINATION_WARNING_THRESHOLD_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate issuedAt;
    private LocalDate dueDate;
    private int dueAmount;
    private int overdueDays;
    private long overdueInterest;
    private boolean terminationWarning;

    private LocalDateTime sentAt;
    private boolean delivered;
    private int attempts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

    private Notice(InsuranceContract contract, BillingStatus status, LocalDate issuedAt) {
        this.contract = contract;
        this.issuedAt = issuedAt;
        this.dueDate = status.oldestUnpaidDueDate();
        this.dueAmount = status.unpaidPrincipal();
        this.overdueDays = (int) status.overdueDays();
        this.overdueInterest = status.overdueInterest();
        this.terminationWarning = status.overdueDays() > TERMINATION_WARNING_THRESHOLD_DAYS;
        this.delivered = false;
        this.attempts = 0;
    }

    /** 미납 계산 결과로 고지서를 만든다. */
    public static Notice of(InsuranceContract contract, BillingStatus status, LocalDate issuedAt) {
        return new Notice(contract, status, issuedAt);
    }

    public boolean isTerminationWarning() {
        return terminationWarning;
    }

    /** 고지서 본문(UC16 3단계: 계약명·납부기한·미납금액·연체이자·납부방법, 30일 초과 시 해지예고). */
    public String buildMessage(String productName) {
        StringBuilder sb = new StringBuilder()
                .append("[미납 보험료 안내] ").append(productName).append("\n")
                .append("납부기한: ").append(dueDate).append("\n")
                .append("미납금액: ").append(dueAmount).append("원\n")
                .append("연체이자: ").append(overdueInterest).append("원\n")
                .append("연체일수: ").append(overdueDays).append("일\n")
                .append("납부방법: 마이페이지 > 보험료 납부에서 결제하실 수 있습니다.\n");
        if (terminationWarning) {
            sb.append("※ 연체가 30일을 초과하여 계약이 해지될 수 있습니다. 조속히 납부해 주십시오.\n");
        }
        return sb.toString();
    }

    /** 발송 결과 기록(UC16 5단계). */
    public void markSent(boolean delivered, int attempts) {
        this.delivered = delivered;
        this.attempts = attempts;
        this.sentAt = LocalDateTime.now();
    }
}

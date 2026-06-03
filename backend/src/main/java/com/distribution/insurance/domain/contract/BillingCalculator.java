package com.distribution.insurance.domain.contract;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 청구 회차를 영속화하지 않고 계약 정보로 미납을 계산한다(ADR 0004). */
public final class BillingCalculator {

    /** 연 10% 연체이율(ADR 0004). */
    private static final double DAILY_RATE = 0.10 / 365;

    private BillingCalculator() {}

    /**
     * 회차 규약: installment k(0-based)의 납부기한은 start.plusMonths(k)이다.
     * 1회차(k=0)의 기한은 계약 시작일(start)과 같다.
     * dueCount는 기한이 asOf 이하인 회차의 1-based 개수다.
     * 미납을 FIFO로 처리하므로, 가장 오래된 미납 회차의 index는 successCount이고
     * 그 기한은 start.plusMonths(successCount)이다.
     */
    public static BillingStatus compute(InsuranceContract contract, long successCount, LocalDate asOf) {
        LocalDate start = contract.getStartDate();
        int totalInstallments = (int) ChronoUnit.MONTHS.between(start, contract.getEndDate());

        int dueCount;
        if (asOf.isBefore(start)) {
            dueCount = 0;
        } else {
            long elapsedMonths = ChronoUnit.MONTHS.between(start, asOf);
            dueCount = (int) Math.min(elapsedMonths + 1, totalInstallments);
        }

        int unpaidCount = (int) Math.max(0, dueCount - successCount);
        if (unpaidCount == 0) {
            return new BillingStatus(0, 0, null, 0, 0);
        }

        int unpaidPrincipal = unpaidCount * contract.getMonthlyPremium();
        LocalDate oldestUnpaidDueDate = start.plusMonths(successCount);
        long overdueDays = asOf.isAfter(oldestUnpaidDueDate)
                ? ChronoUnit.DAYS.between(oldestUnpaidDueDate, asOf) : 0;
        long overdueInterest = Math.round(unpaidPrincipal * overdueDays * DAILY_RATE);

        return new BillingStatus(unpaidCount, unpaidPrincipal, oldestUnpaidDueDate, overdueDays, overdueInterest);
    }
}

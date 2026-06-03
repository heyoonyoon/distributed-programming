package com.distribution.insurance.domain.contract;

import java.time.LocalDate;

/** 한 계약의 미납 계산 결과(온더플라이, ADR 0004). 미납이 없으면 oldestUnpaidDueDate는 null. */
public record BillingStatus(
        int unpaidCount,
        int unpaidPrincipal,
        LocalDate oldestUnpaidDueDate,
        long overdueDays,
        long overdueInterest) {

    public boolean hasUnpaid() {
        return unpaidCount > 0;
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }
}

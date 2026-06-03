package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.Payment;
import com.distribution.insurance.domain.contract.PaymentStatus;

/** 납부 결과. 성공이면 영수증번호, 실패면 사유(E1). */
public record PaymentResultResponse(
        Long paymentId, String status, int amount, String reason) {

    public static PaymentResultResponse success(Payment p) {
        return new PaymentResultResponse(p.getId(), PaymentStatus.SUCCESS.name(), p.getAmount(), null);
    }

    public static PaymentResultResponse failed(Payment p, String reason) {
        return new PaymentResultResponse(p.getId(), PaymentStatus.FAILED.name(), p.getAmount(), reason);
    }
}

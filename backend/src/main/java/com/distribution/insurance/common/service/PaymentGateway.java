package com.distribution.insurance.common.service;

import com.distribution.insurance.contract.domain.PaymentMethod;

/** 외부 결제 처리 추상화. 텍스트 구현은 Mock으로 시뮬레이션(spec). */
public interface PaymentGateway {

    Result charge(PaymentMethod method, int amount, String paymentInfo);

    record Result(boolean success, String reason) {
        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }
}

package com.distribution.insurance.common.service;

/** 보험금을 가입자 계좌로 송금하는 외부 포트(ADR 0008). 수납용 PaymentGateway와 분리. */
public interface BenefitTransferGateway {

    Result transfer(String bankAccount, int amount);

    record Result(boolean success, String reason) {
        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }
}

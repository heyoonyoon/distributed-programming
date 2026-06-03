package com.distribution.insurance.service;

import org.springframework.stereotype.Component;

/**
 * 송금 시뮬레이션. 계좌가 비었거나 "0000"으로 끝나면 실패(UC17 E1), 그 외 성공.
 * 결정적 동작이라 성공/실패 경로를 테스트로 재현할 수 있다.
 */
@Component
public class MockBenefitTransferGateway implements BenefitTransferGateway {

    @Override
    public Result transfer(String bankAccount, int amount) {
        if (bankAccount == null || bankAccount.isBlank()) {
            return Result.fail("계좌 정보가 없습니다.");
        }
        if (bankAccount.endsWith("0000")) {
            return Result.fail("계좌 번호 오류 또는 은행 시스템 문제");
        }
        return Result.ok();
    }
}

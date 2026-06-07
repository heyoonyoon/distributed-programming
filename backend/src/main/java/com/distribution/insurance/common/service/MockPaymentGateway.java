package com.distribution.insurance.common.service;

import com.distribution.insurance.contract.domain.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * 결제 시뮬레이션. 결제정보가 "0000"으로 끝나면 한도초과/잔액부족으로 실패(E1), 그 외 성공.
 * 결정적 동작이라 성공/실패 경로를 테스트로 재현할 수 있다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public Result charge(PaymentMethod method, int amount, String paymentInfo) {
        if (paymentInfo != null && paymentInfo.endsWith("0000")) {
            return Result.fail("한도 초과 또는 잔액 부족");
        }
        return Result.ok();
    }
}

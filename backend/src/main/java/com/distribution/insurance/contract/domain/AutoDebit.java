package com.distribution.insurance.contract.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자동이체 등록 정보(UC10 A1). 전용 엔티티 없이 계약에 임베디드(spec). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AutoDebit {

    private String account;
    private int withdrawalDay;

    public AutoDebit(String account, int withdrawalDay) {
        this.account = account;
        this.withdrawalDay = withdrawalDay;
    }
}

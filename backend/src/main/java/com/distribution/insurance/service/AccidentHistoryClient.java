package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.AccidentHistory;

/** 금융감독원 사고이력 조회(UC15). 텍스트 구현에서는 mock. */
public interface AccidentHistoryClient {
    AccidentHistory fetch(String ssn);
}

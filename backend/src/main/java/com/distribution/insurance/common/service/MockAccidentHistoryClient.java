package com.distribution.insurance.common.service;

import com.distribution.insurance.review.domain.AccidentHistory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 결정적 더미 사고이력. 같은 ssn은 항상 같은 값. */
@Component
public class MockAccidentHistoryClient implements AccidentHistoryClient {

    @Override
    public AccidentHistory fetch(String ssn) {
        int digitSum = 0;
        for (char c : ssn.toCharArray()) {
            if (Character.isDigit(c)) {
                digitSum += c - '0';
            }
        }
        int accidentCount = digitSum % 4;                 // 0~3
        int totalPaid = accidentCount * 1_000_000;
        String license = accidentCount >= 3 ? "SUSPENDED" : "VALID";
        return new AccidentHistory(accidentCount, totalPaid, license, LocalDateTime.now());
    }
}

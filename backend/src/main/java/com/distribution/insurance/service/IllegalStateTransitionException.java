package com.distribution.insurance.service;

/** 허용되지 않은 상태 전이(비PENDING 취소·재심사 등) → 409. */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}

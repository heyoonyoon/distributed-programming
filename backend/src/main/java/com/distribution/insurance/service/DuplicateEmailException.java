package com.distribution.insurance.service;

/** 이미 다른 사용자가 사용 중인 이메일로 변경하려 할 때. → 409 Conflict */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}

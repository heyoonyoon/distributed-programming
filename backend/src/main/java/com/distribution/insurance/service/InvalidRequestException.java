package com.distribution.insurance.service;

/** 도메인 검증 실패(종류-추가정보 불일치, 할증 규칙 위반 등) → 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
